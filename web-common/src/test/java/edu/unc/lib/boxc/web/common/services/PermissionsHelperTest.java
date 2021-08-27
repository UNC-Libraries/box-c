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

import static edu.unc.lib.boxc.auth.api.Permission.editDescription;
import static edu.unc.lib.boxc.auth.api.Permission.viewAccessCopies;
import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.web.common.services.PermissionsHelper;

/**
 *
 * @author bbpennel
 *
 */
public class PermissionsHelperTest {

    private PermissionsHelper helper;

    private ContentObjectSolrRecord mdObject;

    private List<String> roleGroups;

    private AccessGroupSet principals;

    @Mock
    private AccessControlService accessControlService;

    @Before
    public void init() {
        initMocks(this);

        roleGroups = new ArrayList<>();

        mdObject = new ContentObjectSolrRecord();
        mdObject.setId("9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d1");
        mdObject.setRoleGroup(roleGroups);
        List<String> datastreams = Arrays.asList(
                ORIGINAL_FILE.getId() + "|application/pdf|file.pdf|pdf|766|urn:sha1:checksum|",
                JP2_ACCESS_COPY.getId() + "|application/jp2|file.jp2|jp2|884||");
        mdObject.setDatastream(datastreams);

        principals = new AccessGroupSetImpl("group");

        helper = new PermissionsHelper();
        helper.setAccessControlService(accessControlService);
    }

    @Test
    public void testAllowsPublicAccess() {
        roleGroups.add("canViewOriginals|everyone");

        assertTrue("Failed to determine object has full patron access",
                helper.allowsPublicAccess(mdObject));
    }

    @Test
    public void testDoesNotAllowPublicAccess() {
        roleGroups.add("canViewMetadata|everyone");

        assertFalse("Object must not have full patron access",
                helper.allowsPublicAccess(mdObject));
    }

    @Test
    public void testAllowsPublicAccessNoRoleGroups() {
        mdObject.setRoleGroup(null);

        assertFalse("Object must not have full patron access",
                helper.allowsPublicAccess(mdObject));
    }

    @Test
    public void testPermitOriginalAccess() {
        assignPermission(viewOriginal, true);

        assertTrue(helper.hasDatastreamAccess(principals, ORIGINAL_FILE, mdObject));
    }

    @Test
    public void testPermitDerivativeAccess() {
        assignPermission(viewAccessCopies, true);

        assertTrue(helper.hasDatastreamAccess(principals, JP2_ACCESS_COPY, mdObject));
    }

    @Test
    public void testDenyOriginalAccess() {
        assignPermission(viewOriginal, false);

        assertFalse(helper.hasDatastreamAccess(principals, ORIGINAL_FILE, mdObject));
    }

    @Test
    public void testHasEditAccess() {
        assignPermission(editDescription, true);

        assertTrue(helper.hasEditAccess(principals, mdObject));
    }

    @Test
    public void testDoesNotHaveEditAccess() {
        assignPermission(editDescription, false);

        assertFalse(helper.hasEditAccess(principals, mdObject));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHasEditAccessNoPrincipals() {
        assignPermission(editDescription, true);

        assertTrue(helper.hasEditAccess(null, mdObject));
    }

    private void assignPermission(Permission permission, boolean value) {
        when(accessControlService.hasAccess(any(PID.class), eq(principals), eq(permission))).thenReturn(value);
    }
}
