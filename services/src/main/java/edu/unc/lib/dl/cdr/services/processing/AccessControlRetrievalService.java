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

public class AccessControlRetrievalService {
    private InheritedAclFactory aclFactory;
    private RepositoryObjectLoader repoObjLoader;

    public Map<String, Object> getChildPermissions(PID pid) {
        RepositoryObject child = repoObjLoader.getRepositoryObject(pid);

        if (child instanceof ContentContainerObject) {
            List<ContentObject> members = ((ContentContainerObject) child).getMembers();

            ArrayList<Map<String, Object>> memberPermissions = new ArrayList<Map<String,Object>>();

            for (ContentObject member : members) {
                Map<String, Object> result = getPermissions(member.getPid());
                memberPermissions.add(result);
            }

            Map<String, Object> result = new HashMap<String, Object>();
            result.put("memberPermissions", memberPermissions);

            return result;
        }

        return null;
    }

    public Map<String, Object> getPermissions(PID pid) {
        String uuid = pid.getUUID();
        Map<String, Set<String>> principals = aclFactory.getPrincipalRoles(pid);
        boolean markedForDeletion = aclFactory.isMarkedForDeletion(pid);
        Date embargoed = aclFactory.getEmbargoUntil(pid);
        PatronAccess patronAccess = aclFactory.getPatronAccess(pid);

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("uuid", uuid);
        result.put("principals", principals);
        result.put("markForDeletion", markedForDeletion);
        result.put("embargoed", embargoed);
        result.put("patronAccess", patronAccess);

        return result;
    }
}
