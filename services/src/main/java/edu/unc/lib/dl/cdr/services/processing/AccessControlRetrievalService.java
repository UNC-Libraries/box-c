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

import static edu.unc.lib.dl.acl.util.Permission.assignStaffRoles;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;

/**
 * Service that retrieves access-control information for an object and its children, if it has any
 *
 * @author lfarrell
 * @author harring
 *
 */
public class AccessControlRetrievalService {
    private AccessControlService aclService;
    private InheritedAclFactory aclFactory;
    private RepositoryObjectLoader repoObjLoader;

    /**
     * Get the set of permissions that applies to both a given object and its children
     * @param pid
     * @return
     */
    public Map<String, Object> getPermissions(AgentPrincipals agent, PID pid) {
        Map<String, Object> result = getObjectPermissions(agent, pid);
        RepositoryObject parent = repoObjLoader.getRepositoryObject(pid);
        if (parent instanceof ContentContainerObject) {
            List<ContentObject> members = ((ContentContainerObject) parent).getMembers();

            ArrayList<Map<String, Object>> memberPermissions = new ArrayList<Map<String,Object>>();
            for (ContentObject member : members) {
                Map<String, Object> permissions = getObjectPermissions(agent, member.getPid());
                memberPermissions.add(permissions);
            }
            result.put("memberPermissions", memberPermissions);
        }

        return result;
    }

    /**
     * Get the set of permissions that applies to a single object
     * @param pid
     * @return
     */
    private Map<String, Object> getObjectPermissions(AgentPrincipals agent, PID pid) {
        aclService.assertHasAccess("Insufficient privileges to retrieve permissions for object " + pid.getId(),
                pid, agent.getPrincipals(), assignStaffRoles);

        String uuid = pid.getId();
        Map<String, Set<String>> principals = aclFactory.getPrincipalRoles(pid);
        boolean markedForDeletion = aclFactory.isMarkedForDeletion(pid);
        Date embargoed = aclFactory.getEmbargoUntil(pid);
        PatronAccess patronAccess = aclFactory.getPatronAccess(pid);

        Map<String, Object> result = new HashMap<>();

        result.put("pid", uuid);
        result.put("principals", principals);
        result.put("markForDeletion", markedForDeletion);
        result.put("embargoed", embargoed);
        result.put("patronAccess", patronAccess);

        return result;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setAclFactory(InheritedAclFactory aclFactory) {
        this.aclFactory = aclFactory;
    }

    public void setRepoObjLoader(RepositoryObjectLoader repoObjectLoader) {
        this.repoObjLoader = repoObjectLoader;
    }

}
