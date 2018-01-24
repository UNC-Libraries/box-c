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
package edu.unc.lib.dl.cdr.services.processing;

import static edu.unc.lib.dl.acl.util.UserRole.canManage;
import static edu.unc.lib.dl.acl.util.UserRole.canViewMetadata;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author lfarrell
 *
 */
public class AccessControlRetrievalServiceTest {
    private static final String MANAGE_PRINC = "manageGrp";
    private static long ONE_DAY = 86400000;
    private static final String PATRON_PRINC = "everyone";

    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private RepositoryObject repoObj;
    @Mock
    private ContentObject contentObj;
    @Mock
    private InheritedAclFactory aclFactory;
    @Mock
    private PID pid;

    private Map<String, Object> result;
    private AccessControlRetrievalService aclRetrievalService;
    private String uuid;

    @Before
    public void init() {
        initMocks(this);
        aclRetrievalService = new AccessControlRetrievalService(aclFactory, repoObjLoader);
        uuid = UUID.randomUUID().toString();
        when(pid.getUUID()).thenReturn(uuid);
    }

    @Test
    public void getPermissionsTest() {
        Map<String, Set<String>> objPrincRoles = new HashMap<>();
        addPrincipalRoles(objPrincRoles, MANAGE_PRINC, canManage);
        addPrincipalRoles(objPrincRoles, PATRON_PRINC, canViewMetadata);

        Date testDate = new Date(System.currentTimeMillis() + ONE_DAY);

        when(aclFactory.getPrincipalRoles(pid)).thenReturn(objPrincRoles);
        when(aclFactory.isMarkedForDeletion(pid)).thenReturn(false);
        when(aclFactory.getEmbargoUntil(pid)).thenReturn(testDate);
        when(aclFactory.getPatronAccess(pid)).thenReturn(PatronAccess.authenticated);

        result = aclRetrievalService.getPermissions(pid);

        assertTrue(result.containsKey("uuid"));
        assertTrue(result.containsKey("principals"));
        assertTrue(result.containsKey("markForDeletion"));
        assertTrue(result.containsKey("embargoed"));
        assertTrue(result.containsKey("patronAccess"));

        assertTrue(result.containsValue(uuid));
        assertTrue(result.containsValue(objPrincRoles));
        assertTrue(result.containsValue(false));
        assertTrue(result.containsValue(testDate));
        assertTrue(result.containsValue(PatronAccess.authenticated));
    }

    @Test
    public void getMembersPermissionsTest() {
        when(repoObjLoader.getRepositoryObject(pid)).thenReturn(contentObj);
    }

 /*   @Test
    public void getMembersPermissionsNoMembersTest() {
        when(repoObjLoader.getRepositoryObject(pid)).thenReturn(repoObj);
    } */

    private void addPrincipalRoles(Map<String, Set<String>> objPrincRoles,
            String princ, UserRole... roles) {
        Set<String> roleSet = Arrays.stream(roles)
            .map(r -> r.getPropertyString())
            .collect(Collectors.toSet());
        objPrincRoles.put(princ, roleSet);
    }
}
