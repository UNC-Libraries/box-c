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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author lfarrell
 *
 */
public class AccessControlRetrievalService {
    private final InheritedAclFactory aclFactory;
    private final RepositoryObjectLoader repoObjLoader;
    private Map<String, Object> result;

    public AccessControlRetrievalService(InheritedAclFactory aclFactory, RepositoryObjectLoader repoObjLoader) {
        this.aclFactory = aclFactory;
        this.repoObjLoader = repoObjLoader;
    }

    /**
     * Get the set of permissions that apply to the children of a given object
     * @param pid
     * @return
     */
    public Map<String, Object> getMembersPermissions(PID pid) {
        RepositoryObject child = repoObjLoader.getRepositoryObject(pid);

        if (child instanceof ContentContainerObject) {
            List<ContentObject> members = ((ContentContainerObject) child).getMembers();

            ArrayList<Map<String, Object>> memberPermissions = new ArrayList<Map<String,Object>>();

            for (ContentObject member : members) {
                Map<String, Object> permissions = getPermissions(member.getPid());
                memberPermissions.add(permissions);
            }

            result = createMap();
            result.put("memberPermissions", memberPermissions);

            return result;
        }

        return null;
    }

    /**
     * Get the set of permissions that apply to an object
     * @param pid
     * @return
     */
    public Map<String, Object> getPermissions(PID pid) {
        String uuid = pid.getUUID();
        Map<String, Set<String>> principals = aclFactory.getPrincipalRoles(pid);
        boolean markedForDeletion = aclFactory.isMarkedForDeletion(pid);
        Date embargoed = aclFactory.getEmbargoUntil(pid);
        PatronAccess patronAccess = aclFactory.getPatronAccess(pid);

        result = createMap();

        result.put("uuid", uuid);
        result.put("principals", principals);
        result.put("markForDeletion", markedForDeletion);
        result.put("embargoed", embargoed);
        result.put("patronAccess", patronAccess);

        return result;
    }

    private Map<String, Object> createMap() {
        return new HashMap<String, Object>();
    }
}
