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
import edu.unc.lib.boxc.indexing.solr.utils.TechnicalMetadataService;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.PREMIS_V3_NS;

/**
 * Sets fields representing the file format of binaries. Values are set for works and file objects.
 *
 * @author harring
 *
 */
public class SetContentTypeFilter implements IndexDocumentFilter {

    private static final Logger log = LoggerFactory.getLogger(SetContentTypeFilter.class);
    private static final String DEFAULT_MIMETYPE = "application/octet-stream";

    private Properties contentTypeProperties;
    private Properties mimeTypeToDescProperties;
    private Properties extMimetypeOverridesProperties;

    private static final int MAX_FILES_PER_WORK = 10000;
    private static final List<String> WORK_FILE_FIELDS = Arrays.asList(SearchFieldKey.FILE_FORMAT_DESCRIPTION.name(),
            SearchFieldKey.FILE_FORMAT_CATEGORY.name(), SearchFieldKey.FILE_FORMAT_TYPE.name());
    private SolrSearchService solrSearchService;
    private TechnicalMetadataService technicalMetadataService;

    public SetContentTypeFilter() throws IOException {
        contentTypeProperties = new Properties();
        contentTypeProperties.load(new InputStreamReader(getClass().getResourceAsStream(
                "/toContentType.properties")));
        mimeTypeToDescProperties = new Properties();
        mimeTypeToDescProperties.load(new InputStreamReader(getClass().getResourceAsStream(
                "/mimetypeToDescription.properties")));
        extMimetypeOverridesProperties = new Properties();
        extMimetypeOverridesProperties.load(new InputStreamReader(getClass().getResourceAsStream(
                "/extensionMimetypeOverrides.properties")));
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
            String filename = binObj.getFilename();
            String mimetype = getBaseMimeType(binObj.getMimetype());
            if (StringUtils.isBlank(mimetype)) {
                mimetype = DEFAULT_MIMETYPE;
            } else {
                mimetype = overrideMimetype(filename, mimetype);
            }
            String fileDesc = getFormatDescription(fileObj.getPid(), mimetype);
            doc.setFileFormatDescription(fileDesc == null ? null : Collections.singletonList(fileDesc));
            log.debug("The binary {} has mimetype {} and description {}", binObj.getPid(), mimetype, fileDesc);
            doc.setFileFormatType(Collections.singletonList(mimetype));
            ContentCategory contentCategory = getContentCategory(mimetype);
            doc.setFileFormatCategory(Collections.singletonList(contentCategory.getDisplayName()));
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
        var categories = new HashSet<String>();
        var fileTypes = new HashSet<String>();
        var descriptions = new HashSet<String>();
        for (var child: result.getResultList()) {
            if (child.getFileFormatType() != null) {
                fileTypes.addAll(child.getFileFormatType());
            }
            if (child.getFileFormatCategory() != null) {
                categories.addAll(child.getFileFormatCategory());
            }
            if (child.getFileFormatDescription() != null) {
                descriptions.addAll(child.getFileFormatDescription());
            }
        }
        log.debug("Query for children of work {} had categories {} and file types {} from {} files",
                doc.getId(), categories, fileTypes, result.getResultCount());
        doc.setFileFormatCategory(new ArrayList<>(categories));
        doc.setFileFormatType(new ArrayList<>(fileTypes));
        doc.setFileFormatDescription(new ArrayList<>(descriptions));
    }

    /**
     * @param mimetype
     * @return mimetype with encoding and charset params removed
     */
    private String getBaseMimeType(String mimetype) {
        return mimetype == null ? null : mimetype.split(";", 2)[0];
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

    /**
     * Determines if there is a more specific mimetype for a file based on current mimetype and file extension.
     *
     * @param filename
     * @param mimeType
     * @return If there is an override, then it is returned. Otherwise, the original is returned.
     */
    private String overrideMimetype(String filename, String mimeType) {
        if (filename == null || mimeType == null) {
            return mimeType;
        }
        String ext = FilenameUtils.getExtension(filename);
        if (StringUtils.isBlank(ext)) {
            return mimeType;
        }
        String overrideKey = ext.toLowerCase().replaceAll(",", "") + "," + mimeType.toLowerCase();
        String overrideVal = extMimetypeOverridesProperties.getProperty(overrideKey);
        return overrideVal == null ? mimeType : overrideVal;
    }

    /**
     * Retrieve the format description, a human readable format description, from vocab or FITS datastream
     * @param filePid
     * @return
     */
    private String getFormatDescription(PID filePid, String mimeType) {
        // If there is a preferred term for the given mimetype from our vocabulary, use that
        String formatName = mimeTypeToDescProperties.getProperty(mimeType);
        if (formatName != null) {
            return formatName;
        }
        // No preferred term, so pull value from FITS
        try {
            var techMdDoc = technicalMetadataService.retrieveDocument(filePid);
            formatName = techMdDoc.getRootElement().getChild("object", PREMIS_V3_NS)
                    .getChild("objectCharacteristics", PREMIS_V3_NS)
                    .getChild("format", PREMIS_V3_NS)
                    .getChild("formatDesignation", PREMIS_V3_NS)
                    .getChildTextTrim("formatName", PREMIS_V3_NS);
            if (!StringUtils.isEmpty(formatName) && !"Unknown Binary".equals(formatName)) {
                return formatName;
            }
        } catch (RepositoryException | FedoraException e) {
            log.warn("Unable to retrieve techmd datastream for {}", filePid, e);
        } catch (NullPointerException e) {
            log.warn("Invalid techmd datastream, unable to extract formatName for {}", filePid, e);
        }

        // No useful description could be determined
        return null;
    }

    private ContentCategory getContentCategory(String mimetype) {
        if (mimetype == null) {
            return ContentCategory.unknown;
        }
        // Get mapped category based on mimetype if exists
        String contentCategory = (String) contentTypeProperties.get(mimetype);
        if (contentCategory != null) {
            var category = ContentCategory.getContentCategory(contentCategory);
            if (category == null) {
                log.error("Misconfiguration, mimetype {} returned invalid category {}", mimetype, contentCategory);
            } else {
                return category;
            }
        }

        // Fall back to mapping category based on first half of the mimetype
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
        return ContentCategory.unknown;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    public void setTechnicalMetadataService(TechnicalMetadataService technicalMetadataService) {
        this.technicalMetadataService = technicalMetadataService;
    }
}
