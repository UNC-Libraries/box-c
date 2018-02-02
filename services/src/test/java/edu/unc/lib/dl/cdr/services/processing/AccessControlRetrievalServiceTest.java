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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.fcrepo4.ObjectAclFactory;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author lfarrell
 * @author harring
 *
 */
public class AccessControlRetrievalServiceTest {
    private static final String MANAGE_PRINC = "manageGrp";
    private static long ONE_DAY = 86400000;
    private static final String PATRON_PRINC = "everyone";

    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private ContentObject member;
    @Mock
    private ContentContainerObject parent;
    @Mock
    private ObjectAclFactory objAclFactory;
    @Mock
    private InheritedAclFactory inheritedAclFactory;
    @Mock
    private AccessControlService aclService;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private ArrayList<Map<String, Object>> memberPermissions;
    @Mock
    private  Map<String, Object> permissions;

    private PID parentPid;
    private PID memberPid;
    private String memberUuid;
    private String parentUuid;
    private Map<String, Object> result;
    private AccessControlRetrievalService aclRetrievalService;
    private Map<String, Set<String>> objPrincRoles;
    private Date testDate;

    @Before
    public void init() {
        initMocks(this);

        parentPid = PIDs.get(UUID.randomUUID().toString());
        memberPid = PIDs.get(UUID.randomUUID().toString());
        memberUuid = memberPid.getId();
        parentUuid = parentPid.getId();
        testDate = new Date(System.currentTimeMillis() + ONE_DAY);

        objPrincRoles = new HashMap<>();
        addPrincipalRoles(objPrincRoles, MANAGE_PRINC, canManage);
        addPrincipalRoles(objPrincRoles, PATRON_PRINC, canViewMetadata);

        aclRetrievalService = new AccessControlRetrievalService();
        aclRetrievalService.setObjectAclFactory(objAclFactory);
        aclRetrievalService.setInheritedAclFactory(inheritedAclFactory);
        aclRetrievalService.setAclService(aclService);
        aclRetrievalService.setRepoObjLoader(repoObjLoader);

        when(objAclFactory.isMarkedForDeletion(any(PID.class))).thenReturn(false);
        when(objAclFactory.getPrincipalRoles(any(PID.class))).thenReturn(objPrincRoles);
        when(objAclFactory.getEmbargoUntil(any(PID.class))).thenReturn(testDate);
        when(objAclFactory.getPatronAccess(any(PID.class))).thenReturn(PatronAccess.authenticated);
        when(inheritedAclFactory.isMarkedForDeletion(any(PID.class))).thenReturn(false);
        when(inheritedAclFactory.getPrincipalRoles(any(PID.class))).thenReturn(objPrincRoles);
        when(inheritedAclFactory.getEmbargoUntil(any(PID.class))).thenReturn(testDate);
        when(inheritedAclFactory.getPatronAccess(any(PID.class))).thenReturn(PatronAccess.authenticated);
    }

    @Test
    public void getPermissionsObjectWithoutChildrenTest() throws Exception {
        when(repoObjLoader.getRepositoryObject(memberPid)).thenReturn(member);

        result = aclRetrievalService.getPermissions(agent, memberPid);

        assertEquals(memberUuid, result.get("pid"));
        assertEquals(false, result.get("markForDeletion"));
        assertEquals(testDate, result.get("embargo"));
        assertEquals(PatronAccess.authenticated, result.get("patronAccess"));
        assertFalse(result.containsKey("memberPermissions"));
    }

    @Test
    public void getPermissionsContainerWithoutChildrenTest() throws Exception {
        when(repoObjLoader.getRepositoryObject(parentPid)).thenReturn(parent);

        result = aclRetrievalService.getPermissions(agent, parentPid);

        assertEquals(parentUuid, result.get("pid"));
        assertEquals(false, result.get("markForDeletion"));
        assertEquals(testDate, result.get("embargo"));
        assertEquals(PatronAccess.authenticated, result.get("patronAccess"));
        assertFalse(result.containsKey("memberPermissions"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getPermissionsObjectWithChildrenTest() throws Exception {
        List<ContentObject> children = new ArrayList<>();
        children.add(member);

        when(repoObjLoader.getRepositoryObject(parentPid)).thenReturn(parent);
        when(parent.getMembers()).thenReturn(children);
        when(member.getPid()).thenReturn(memberPid);
        when(memberPermissions.add(result)).thenReturn(true);
        // set embargo on child but not on parent
        when(objAclFactory.getEmbargoUntil(parentPid)).thenReturn(null);
        when(objAclFactory.getEmbargoUntil(memberPid)).thenReturn(testDate);
        when(inheritedAclFactory.getEmbargoUntil(any(PID.class))).thenReturn(null);

        result = aclRetrievalService.getPermissions(agent, parentPid);

        assertEquals(parentUuid, result.get("pid"));
        assertEquals(false, result.get("markForDeletion"));
        assertNull(result.get("embargo"));
        assertEquals(objPrincRoles, result.get("principals"));
        assertEquals(PatronAccess.authenticated, result.get("patronAccess"));
        assertTrue(result.containsKey("memberPermissions"));

        Map<String,Object> memberValues = ((List<Map<String, Object>>) result.get("memberPermissions")).get(0);

        assertEquals(memberUuid, memberValues.get("pid"));
        assertEquals(false, memberValues.get("markForDeletion"));
        assertEquals(testDate, memberValues.get("embargo"));
        assertEquals(objPrincRoles, memberValues.get("principals"));
        assertEquals(PatronAccess.authenticated, memberValues.get("patronAccess"));
    }

    @Test(expected=AccessRestrictionException.class)
    public void userNotAuthorizedTest() {
        doThrow(new AccessRestrictionException()).when(aclService).assertHasAccess(anyString(), any(PID.class),
                any(AccessGroupSet.class), any(Permission.class));

        result = aclRetrievalService.getPermissions(agent, memberPid);
    }

    private void addPrincipalRoles(Map<String, Set<String>> objPrincRoles,
            String princ, UserRole... roles) {
        Set<String> roleSet = Arrays.stream(roles)
            .map(r -> r.getPropertyString())
            .collect(Collectors.toSet());
        objPrincRoles.put(princ, roleSet);
    }
}
