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
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author lfarrell
 */
public class AccessCopiesServiceTest {
    private PermissionsHelper helper;

    private ContentObjectSolrRecord mdObject;

    private ContentObjectSolrRecord mdObjectImg;

    private AccessGroupSet principals;

    private AccessCopiesService accessCopiesService;

    @Mock
    private AccessControlService accessControlService;

    @Before
    public void init() throws IOException {
        initMocks(this);

        mdObject = new ContentObjectSolrRecord();
        mdObject.setId("9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d1");
        List<String> datastreams = Collections.singletonList(
                ORIGINAL_FILE.getId() + "|application/pdf|file.pdf|pdf|766|urn:sha1:checksum|");
        mdObject.setDatastream(datastreams);

        mdObjectImg = new ContentObjectSolrRecord();
        mdObjectImg.setId("27f8d1c5-14a1-4ed3-b0c0-6da67fa5f6d1");
        List<String> imgDatastreams = Collections.singletonList(
                ORIGINAL_FILE.getId() + "|image/png|file.png|png|766|urn:sha1:checksum|");
        mdObjectImg.setDatastream(imgDatastreams);

        principals = new AccessGroupSetImpl("group");

        helper = new PermissionsHelper();
        helper.setAccessControlService(accessControlService);

        accessCopiesService = new AccessCopiesService();
        accessCopiesService.setPermissionsHelper(helper);
    }

    @Test
    public void testHasViewAblePdf() {
        hasPermissions(mdObject, true);
        assertTrue("Work does not have PDF viewable content",
                accessCopiesService.hasViewablePdf(mdObject, principals));
    }

    @Test
    public void testDoesNotHasViewAblePdf() {
        hasPermissions(mdObjectImg, true);
        assertFalse("Work has viewable PDF content",
                accessCopiesService.hasViewablePdf(mdObjectImg, principals));
    }

    @Test
    public void testNoPermissionsHasViewAblePdf() {
        hasPermissions(mdObject, false);
        assertFalse("Work has viewable PDF content",
                accessCopiesService.hasViewablePdf(mdObject, principals));
    }

    private void hasPermissions(ContentObjectSolrRecord contentObject, boolean hasAccess) {
        when(accessControlService.hasAccess(contentObject.getPid(), principals, viewOriginal)).thenReturn(hasAccess);
    }
}
