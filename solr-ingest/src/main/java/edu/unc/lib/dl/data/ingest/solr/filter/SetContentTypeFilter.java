/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.data.ingest.solr.filter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.search.solr.util.ContentCategory;

/**
 * Assigns content-type field for a Solr record. The field contains a category (e.g., "image")
 * and an extension (e.g., ".jpg") for binary content that is contained by the represented object.
 *
 * @author harring
 *
 */
public class SetContentTypeFilter implements IndexDocumentFilter {

    private static final Logger log = LoggerFactory.getLogger(SetContentTypeFilter.class);
    private static final Pattern EXTENSION_REGEX =
            Pattern.compile("^[^\\n]*[^.]\\.(\\d*[a-zA-Z][a-zA-Z0-9]*)[\"'{}, _\\-`()*]*$");
    private static final int EXTENSION_LIMIT = 8;

    private Properties mimetypeToExtensionMap;
    private Properties contentTypeProperties;

    public SetContentTypeFilter() throws IOException {
        mimetypeToExtensionMap = new Properties();
        mimetypeToExtensionMap.load(new InputStreamReader(this.getClass().getResourceAsStream(
                "mimetypeToExtension.txt")));
        contentTypeProperties = new Properties();
        contentTypeProperties.load(new InputStreamReader(this.getClass().getResourceAsStream(
                "toContentType.properties")));
    }

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        ContentObject contentObj = dip.getContentObject();
        // object being indexed must be a work or a file object
        if (!(contentObj instanceof WorkObject) && !(contentObj instanceof FileObject)) {
                return;
        }
        FileObject fileObj = getFileObject(dip);
        BinaryObject binObj;
        if (fileObj == null) {
                return;
        }
        binObj = fileObj.getOriginalFile();
        String filepath = binObj.getFilename();
        String mimetype = binObj.getMimetype();
        log.debug("The binary has filepath {} and mimetype {}", filepath, mimetype);
        ArrayList<String> contentTypes = new ArrayList<>();
        extractContentType(filepath, mimetype, contentTypes);
        dip.getDocument().setContentType(contentTypes);
    }

    private FileObject getFileObject(DocumentIndexingPackage dip) throws IndexingException {
        ContentObject obj = dip.getContentObject();
        FileObject fileObj;
        if (obj instanceof WorkObject) {
            fileObj = ((WorkObject) obj).getPrimaryObject();
        } else {
            fileObj = (FileObject) obj;
        }
        return fileObj;
    }

    private String getExtension(String filepath, String mimetype) {
        if (filepath != null) {
            Matcher matcher = EXTENSION_REGEX.matcher(filepath);
            if (matcher.matches()) {
                String extension = matcher.group(1);
                if (extension.length() <= EXTENSION_LIMIT) {
                    return extension.toLowerCase();
                }
            }
        }
        if (mimetype != null) {
            String extension = mimetypeToExtensionMap.getProperty(mimetype);
            return extension;
        }
        return null;
    }

    private void extractContentType(String filepath, String mimetype, List<String> contentTypes) {
        ContentCategory contentCategory = getContentCategory(mimetype, getExtension(filepath, mimetype));
        // add string with name + display-name to list of content types
        contentTypes.add('^' + contentCategory.getJoined());
        StringBuilder contentType = new StringBuilder();
        contentType.append('/').append(contentCategory.name()).append('^');
        String extension = getExtension(filepath, mimetype);
        if (extension == null) {
            contentType.append("unknown,unknown");
        } else {
            contentType.append(extension).append(',').append(extension);
        }
        // add string with content category name + extension to list of content types
        contentTypes.add(contentType.toString());
    }

    private ContentCategory getContentCategory(String mimetype, String extension) {
        if (mimetype == null) {
            return ContentCategory.unknown;
        }
        int index = mimetype.indexOf('/');
        if (index != -1) {
            String mimetypeType = mimetype.substring(0, index);
            if (mimetypeType.equals("image")) {
                return ContentCategory.image;
            }
            if (mimetypeType.equals("video")) {
                return ContentCategory.video;
            }
            if (mimetypeType.equals("audio")) {
                return ContentCategory.audio;
            }
            if (mimetypeType.equals("text")) {
                return ContentCategory.text;
            }
        }

        String contentCategory = (String) contentTypeProperties.get("mime." + mimetype);
        contentCategory = (String) contentTypeProperties.get("ext." + extension);

        return ContentCategory.getContentCategory(contentCategory);
    }

}
