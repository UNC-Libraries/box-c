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
package edu.unc.lib.boxc.web.common.services;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;
import edu.unc.lib.boxc.search.solr.utils.FacetFieldUtil;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author lfarrell
 */
public class AccessCopiesServiceTest  {
    private PermissionsHelper helper;

    private ContentObjectSolrRecord mdObject;

    private ContentObjectSolrRecord mdObjectImg;

    private ContentObjectSolrRecord mdObjectAudio;

    private ContentObjectSolrRecord noOriginalFileObj;

    private ContentObjectSolrRecord mdObjectXml;

    private AccessGroupSet principals;

    private AccessCopiesService accessCopiesService;

    private final String pdfMimetype = "application/(x-)?pdf";

    @Mock
    private AccessControlService accessControlService;
    @Mock
    private AccessRestrictionUtil restrictionUtil;
    @Mock
    private SolrSettings solrSettings;
    @Mock
    private FacetFieldUtil facetFieldUtil;
    @Mock
    private SolrClient solrClient;
    @Mock
    private QueryResponse queryResponse;
    @Mock
    private SolrDocumentList solrDocumentList;

    @Before
    public void init() throws IOException, SolrServerException {
        initMocks(this);

        mdObject = new ContentObjectSolrRecord();
        mdObject.setResourceType(ResourceType.Work.name());
        mdObject.setId(UUID.randomUUID().toString());
        List<String> datastreams = Collections.singletonList(
                ORIGINAL_FILE.getId() + "|application/pdf|file.pdf|pdf|766|urn:sha1:checksum|");
        mdObject.setDatastream(datastreams);

        mdObjectImg = new ContentObjectSolrRecord();
        mdObjectImg.setResourceType(ResourceType.Work.name());
        mdObjectImg.setId(UUID.randomUUID().toString());
        List<String> imgDatastreams = Collections.singletonList(
                ORIGINAL_FILE.getId() + "|image/png|file.png|png|766|urn:sha1:checksum|");
        mdObjectImg.setDatastream(imgDatastreams);

        mdObjectAudio = new ContentObjectSolrRecord();
        mdObjectAudio.setResourceType(ResourceType.Work.name());
        mdObjectAudio.setId(UUID.randomUUID().toString());
        List<String> audioDatastreams = Collections.singletonList(
                ORIGINAL_FILE.getId() + "|audio/mpeg|file.mp3|mp3|766|urn:sha1:checksum|");
        mdObjectAudio.setDatastream(audioDatastreams);

        noOriginalFileObj = new ContentObjectSolrRecord();
        noOriginalFileObj.setResourceType(ResourceType.Work.name());
        noOriginalFileObj.setId("45c8d1c5-14a1-4ed3-b0c0-6da67fa5f6d1");

        mdObjectXml = new ContentObjectSolrRecord();
        mdObjectXml.setResourceType(ResourceType.Work.name());
        mdObjectXml.setId(UUID.randomUUID().toString());
        List<String> xmlDatastreams = Collections.singletonList(
                TECHNICAL_METADATA.getId() + "|text.xml|file.xml|xml|766|urn:sha1:checksum|");
        mdObjectXml.setDatastream(xmlDatastreams);

        principals = new AccessGroupSetImpl("group");

        helper = new PermissionsHelper();
        helper.setAccessControlService(accessControlService);

        Properties searchProps = new Properties();
        searchProps.load(this.getClass().getResourceAsStream("/search.properties"));
        SearchSettings searchSettings = new SearchSettings();
        searchSettings.setProperties(searchProps);

        accessCopiesService = new AccessCopiesService();
        accessCopiesService.setPermissionsHelper(helper);
        accessCopiesService.setAccessRestrictionUtil(restrictionUtil);
        accessCopiesService.setSolrSettings(solrSettings);
        accessCopiesService.setSearchSettings(searchSettings);
        accessCopiesService.setFacetFieldUtil(facetFieldUtil);
        accessCopiesService.setSolrClient(solrClient);

        when(solrClient.query(any(SolrQuery.class))).thenReturn(queryResponse);
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        when(solrDocumentList.getNumFound()).thenReturn(1L);
    }

    @Test
    public void testHasViewablePdf() {
        hasPermissions(mdObject, true);
        assertTrue("Work does not have PDF viewable content",
                accessCopiesService.hasDatastreamContent(mdObject, principals, pdfMimetype));
    }

    @Test
    public void testDoesNotHaveViewablePdf() {
        hasPermissions(mdObjectImg, true);
        assertFalse("Work has viewable PDF content",
                accessCopiesService.hasDatastreamContent(mdObjectImg, principals, pdfMimetype));
    }

    @Test
    public void testNoPermissionsHasViewablePdf() {
        hasPermissions(mdObject, false);
        assertFalse("Work has viewable PDF content",
                accessCopiesService.hasDatastreamContent(mdObject, principals, pdfMimetype));
    }

    @Test
    public void testHasViewablePdfPid() {
        hasPermissions(mdObject, true);

        List<ContentObjectSolrRecord> mdObjects = Collections.singletonList(mdObject);
        when(queryResponse.getBeans(ContentObjectSolrRecord.class)).thenReturn(mdObjects);

        String filePid = accessCopiesService.getDatastreamPid(mdObject, principals, pdfMimetype);
        assertNotNull(filePid);
        assertEquals(filePid, mdObject.getId());
    }

    @Test
    public void testDoesNotHaveViewablePdfPidOneContentObject() {
        hasPermissions(mdObjectImg, true);

        List<ContentObjectSolrRecord> mdObjects = Collections.singletonList(mdObjectImg);
        when(queryResponse.getBeans(ContentObjectSolrRecord.class)).thenReturn(mdObjects);

        String filePid = accessCopiesService.getDatastreamPid(mdObjectImg, principals, pdfMimetype);
        assertNull(filePid);
    }

    @Test
    public void testDoesNotHaveViewablePdfPidMultipleFileObjects() {
        hasPermissions(mdObjectImg, true);

        when(solrDocumentList.getNumFound()).thenReturn(2L);

        String filePid = accessCopiesService.getDatastreamPid(mdObjectImg, principals, pdfMimetype);
        assertNull(filePid);
    }

    @Test
    public void testPrimaryObjectHasDownloadUrl() {
        hasPermissions(mdObjectAudio, true);
        String downloadUrl = accessCopiesService.getDownloadUrl(mdObjectAudio, principals);
        assertEquals("content/" + mdObjectAudio.getId(), downloadUrl);
    }

    @Test
    public void testChildObjectHasDownloadUrl() {
        hasPermissions(noOriginalFileObj, true);
        hasPermissions(mdObject, true);

        List<ContentObjectSolrRecord> resultList = Collections.singletonList(mdObject);
        when(queryResponse.getBeans(ContentObjectSolrRecord.class)).thenReturn(resultList);

        String downloadUrl = accessCopiesService.getDownloadUrl(noOriginalFileObj, principals);
        assertEquals("indexablecontent/" + mdObject.getId(), downloadUrl);
    }

    @Test
    public void testDoesNotHaveDownloadUrl() {
        hasPermissions(noOriginalFileObj, true);
        hasPermissions(mdObjectXml, true);

        List<ContentObjectSolrRecord> resultList = Collections.singletonList(mdObjectXml);
        when(queryResponse.getBeans(ContentObjectSolrRecord.class)).thenReturn(resultList);

        String downloadUrl = accessCopiesService.getDownloadUrl(noOriginalFileObj, principals);

        assertEquals("", downloadUrl);
    }

    @Test
    public void testMultipleFileObjectsDoesNotHaveDownloadUrl() {
        hasPermissions(noOriginalFileObj, true);
        when(solrDocumentList.getNumFound()).thenReturn(2L);

        String downloadUrl = accessCopiesService.getDownloadUrl(noOriginalFileObj, principals);

        assertEquals("", downloadUrl);
    }

    @Test
    public void hasPlayableAudiofile() {
        hasPermissions(mdObjectAudio, true);
        assertTrue("Work has no audio content",
                accessCopiesService.hasPlayableAudio(mdObjectAudio, principals));
    }

    @Test
    public void hasPlayableAudiofileNoPrimaryObject() {
        hasPermissions(mdObjectAudio, true);
        hasPermissions(mdObjectXml, true);
        List<ContentObjectSolrRecord> resultList = Collections.singletonList(mdObjectAudio);
        when(queryResponse.getBeans(ContentObjectSolrRecord.class)).thenReturn(resultList);
        assertTrue("Work has no audio content",
                accessCopiesService.hasPlayableAudio(mdObjectXml, principals));
    }

    @Test
    public void doesNotHavePlayableAudiofile() {
        hasPermissions(mdObjectImg, true);
        hasPermissions(mdObjectXml, true);
        List<ContentObjectSolrRecord> resultList = Collections.singletonList(mdObjectXml);
        when(queryResponse.getBeans(ContentObjectSolrRecord.class)).thenReturn(resultList);
        assertFalse("Work has audio content",
                accessCopiesService.hasPlayableAudio(mdObjectImg, principals));
    }

    private void hasPermissions(ContentObjectSolrRecord contentObject, boolean hasAccess) {
        when(accessControlService.hasAccess(contentObject.getPid(), principals, viewOriginal)).thenReturn(hasAccess);
    }
}
