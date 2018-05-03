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
import edu.unc.lib.dl.acl.fcrepo4.ObjectAclFactory;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import io.dropwizard.metrics5.Timer;

/**
 * Service that retrieves access-control information for an object and its children, if it has any
 *
 * @author lfarrell
 * @author harring
 *
 */
public class AccessControlRetrievalService {
    private AccessControlService aclService;
    private ObjectAclFactory objAclFactory;
    private InheritedAclFactory inheritedAclFactory;
    private RepositoryObjectLoader repoObjLoader;

    private static final Timer timer = TimerFactory.createTimerForClass(AccessControlRetrievalService.class);

    /**
     * Get the set of permissions that applies to both a given object and its children
     *
     * @param agent the agent's authentication principals
     * @param pid the object's pid
     * @return a map of permissions for an object and its children
     */
    public Map<String, Object> getPermissions(AgentPrincipals agent, PID pid) {
        Map<String, Object> result = null;
        try (Timer.Context context = timer.time()) {
            result = getObjectPermissions(agent, pid);
            RepositoryObject parent = repoObjLoader.getRepositoryObject(pid);
            if (parent instanceof ContentContainerObject) {
                List<ContentObject> members = ((ContentContainerObject) parent).getMembers();

                if (!members.isEmpty()) {
                    List<Map<String, Object>> memberPermissions = new ArrayList<>();
                    for (ContentObject member : members) {
                        Map<String, Object> permissions = getObjectPermissions(agent, member.getPid());
                        memberPermissions.add(permissions);
                    }
                    result.put("memberPermissions", memberPermissions);
                }
            }
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
        Map<String, Set<String>> principals = objAclFactory.getPrincipalRoles(pid);
        boolean markedForDeletion = objAclFactory.isMarkedForDeletion(pid);
        Date embargo = objAclFactory.getEmbargoUntil(pid);
        PatronAccess patronAccess = objAclFactory.getPatronAccess(pid);

        Map<String, Set<String>> inheritedPrincipals = inheritedAclFactory.getPrincipalRoles(pid);
        boolean inheritedMarkedForDeletion = inheritedAclFactory.isMarkedForDeletion(pid);
        Date inheritedEmbargo = inheritedAclFactory.getEmbargoUntil(pid);
        PatronAccess inheritedPatronAccess = inheritedAclFactory.getPatronAccess(pid);

        Map<String, Object> result = new HashMap<>();

        result.put("pid", uuid);
        result.put("principals", principals);
        result.put("markForDeletion", markedForDeletion);
        result.put("embargo", embargo);
        result.put("patronAccess", patronAccess);

        result.put("inheritedPrincipals", inheritedPrincipals);
        result.put("inheritedMarkForDeletion", inheritedMarkedForDeletion);
        result.put("inheritedEmbargo", inheritedEmbargo);
        result.put("inheritedPatronAccess", inheritedPatronAccess);

        return result;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setObjectAclFactory(ObjectAclFactory objAclFactory) {
        this.objAclFactory = objAclFactory;
    }

    public void setInheritedAclFactory(InheritedAclFactory inheritedAclFactory) {
        this.inheritedAclFactory = inheritedAclFactory;
    }

    public void setRepoObjLoader(RepositoryObjectLoader repoObjectLoader) {
        this.repoObjLoader = repoObjectLoader;
    }

}
