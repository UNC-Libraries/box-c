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
import static org.junit.Assert.assertTrue;
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

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
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
    private InheritedAclFactory aclFactory;
    @Mock
    private AccessControlService aclService;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private ArrayList<Map<String, Object>> memberPermissions;
    @Mock
    private  Map<String, Object> permissions;
    @Mock
    private Map<String, Object> permResult;

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
        aclRetrievalService.setAclFactory(aclFactory);
        aclRetrievalService.setAclService(aclService);
        aclRetrievalService.setRepoObjLoader(repoObjLoader);
    }

    @Test
    public void getPermissionsObjectWithoutChildrenTest() throws Exception {


        when(aclFactory.getPrincipalRoles(memberPid)).thenReturn(objPrincRoles);
        when(aclFactory.isMarkedForDeletion(memberPid)).thenReturn(false);
        when(aclFactory.getEmbargoUntil(memberPid)).thenReturn(testDate);
        when(aclFactory.getPatronAccess(memberPid)).thenReturn(PatronAccess.authenticated);
        when(repoObjLoader.getRepositoryObject(memberPid)).thenReturn(member);

        result = aclRetrievalService.getPermissions(agent, memberPid);
        String resultJson = generateJson(result);

        assertTrue(resultJson.contains("pid\":\"" + memberUuid));
        assertTrue(resultJson.contains("markForDeletion\":false"));
        assertTrue(resultJson.contains("embargoed\":" + testDate.getTime()));
        assertTrue(resultJson.contains("patronAccess\":\"authenticated"));
        assertFalse(resultJson.contains("memberPermissions"));
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

        ArrayList<Map<String, Object>> memberPerms = new ArrayList<Map<String,Object>>();
        when(permResult.put("memberPermissions", memberPermissions)).thenReturn(memberPerms);

        when(aclFactory.getPrincipalRoles(memberPid)).thenReturn(objPrincRoles);
        when(aclFactory.isMarkedForDeletion(memberPid)).thenReturn(true);
        when(aclFactory.getEmbargoUntil(memberPid)).thenReturn(testDate);
        when(aclFactory.getPatronAccess(memberPid)).thenReturn(PatronAccess.authenticated);

        result = aclRetrievalService.getPermissions(agent, parentPid);
        String resultJson = generateJson(result);

        assertTrue(resultJson.contains("pid\":\"" + parentUuid));
        assertTrue(resultJson.contains("memberPermissions"));
        assertTrue(resultJson.contains("markForDeletion\":false"));
        assertTrue(resultJson.contains("embargoed\":" + testDate.getTime()));
        assertTrue(resultJson.contains("patronAccess\":\"authenticated"));

        memberPerms.add(result);
        Map<String,Object> returnedValues = ((ArrayList<Map<String, Object>>) result.get("memberPermissions")).get(0);

        assertEquals(memberUuid, returnedValues.get("pid"));
        assertEquals(objPrincRoles, returnedValues.get("principals"));
        assertEquals(true, returnedValues.get("markForDeletion"));
        assertEquals(testDate, returnedValues.get("embargoed"));
        assertEquals(PatronAccess.authenticated, returnedValues.get("patronAccess"));
    }

    private void addPrincipalRoles(Map<String, Set<String>> objPrincRoles,
            String princ, UserRole... roles) {
        Set<String> roleSet = Arrays.stream(roles)
            .map(r -> r.getPropertyString())
            .collect(Collectors.toSet());
        objPrincRoles.put(princ, roleSet);
    }
    private String generateJson(Map<String, Object> resultMap) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(resultMap);
    }

}
