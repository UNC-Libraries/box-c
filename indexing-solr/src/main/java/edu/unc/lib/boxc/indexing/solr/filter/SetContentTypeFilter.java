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
package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.utils.ContentTypeUtils;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.search.api.ContentCategory;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    private static final int MAX_FILES_PER_WORK = 10000;
    private static final List<String> WORK_FILE_FIELDS = Arrays.asList(SearchFieldKey.CONTENT_TYPE.name());
    private SolrSearchService solrSearchService;

    public SetContentTypeFilter() throws IOException {
        mimetypeToExtensionMap = new Properties();
        mimetypeToExtensionMap.load(new InputStreamReader(getClass().getResourceAsStream(
                "/mimetypeToExtension.txt")));
        contentTypeProperties = new Properties();
        contentTypeProperties.load(new InputStreamReader(getClass().getResourceAsStream(
                "/toContentType.properties")));
    }

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        var contentObj = dip.getContentObject();
        var doc = dip.getDocument();
        if (contentObj instanceof WorkObject) {
            log.debug("Indexing contentType of work {}", dip.getPid().getId());
            addFileContentTypesToWork(doc);
        } else if (contentObj instanceof FileObject) {
            var fileObj = (FileObject) contentObj;
            BinaryObject binObj = fileObj.getOriginalFile();
            String filepath = binObj.getFilename();
            String mimetype = binObj.getMimetype();
            log.debug("The binary {} has filepath {} and mimetype {}", binObj.getPid(), filepath, mimetype);
            List<String> contentTypes = new ArrayList<>();
            extractContentType(filepath, mimetype, contentTypes);
            dip.getDocument().setContentType(contentTypes);
        }
    }

    private void addFileContentTypesToWork(IndexDocumentBean doc) {
        var searchState = new SearchState();
        var objectPath = getObjectPath(doc);
        searchState.setFacet(objectPath);
        searchState.setFacet(new GenericFacet(SearchFieldKey.RESOURCE_TYPE.name(), ResourceType.File.name()));
        searchState.setRowsPerPage(MAX_FILES_PER_WORK);
        searchState.setResultFields(WORK_FILE_FIELDS);
        var searchRequest = new SearchRequest();
        searchRequest.setSearchState(searchState);
        var result = solrSearchService.getSearchResults(searchRequest);
        var contentTypes = result.getResultList().stream()
                .flatMap(r -> r.getContentType().stream())
                .distinct()
                .collect(Collectors.toList());
        log.debug("Query for children of work {} had contentTypes {} from {} files",
                doc.getId(), contentTypes, result.getResultCount());
        doc.setContentType(contentTypes);
    }

    private CutoffFacet getObjectPath(IndexDocumentBean doc) {
        CutoffFacetImpl ancestorPath;
        if (doc.getAncestorPath() != null && !doc.getAncestorPath().isEmpty()) {
            ancestorPath = new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), doc.getAncestorPath(), -1);
        } else {
            ancestorPath = (CutoffFacetImpl) solrSearchService.getAncestorPath(doc.getId(), null);
        }
        ancestorPath.addNode(doc.getId());
        return ancestorPath;
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
            return mimetypeToExtensionMap.getProperty(mimetype);
        }
        return null;
    }

    private void extractContentType(String filepath, String mimetype, List<String> contentTypes) {
        String extension = getExtension(filepath, mimetype);
        ContentCategory contentCategory = getContentCategory(mimetype, extension);
        ContentTypeUtils.addContentTypeFacets(contentCategory, extension, contentTypes);
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
        if (contentCategory == null) {
            contentCategory = (String) contentTypeProperties.get("ext." + extension);
        }

        return ContentCategory.getContentCategory(contentCategory);
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }
}
