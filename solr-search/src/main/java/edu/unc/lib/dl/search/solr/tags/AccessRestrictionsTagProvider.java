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
package edu.unc.lib.dl.search.solr.tags;

import java.util.Date;
import java.util.List;
import java.util.Set;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.Tag;

/**
 *
 * @author bbpennel
 *
 */
@Deprecated
public class AccessRestrictionsTagProvider implements TagProvider {

    private static final String[] PUBLIC = new String[] { "public" };

    @Override
    public void addTags(BriefObjectMetadata record, AccessGroupSet accessGroups) {

        ObjectAccessControlsBean acls = record.getAccessControlBean();
        if (acls == null) {
            return;
        }

        // public
        Set<UserRole> publicRoles = acls.getRoles(new AccessGroupSet(PUBLIC));
        if (publicRoles.contains(UserRole.patron)) {
            record.addTag(new Tag("public", "patron"));
        } else if (publicRoles.contains(UserRole.metadataPatron)) {
            record.addTag(new Tag("public", "metadata"));
        } else if (publicRoles.contains(UserRole.accessCopiesPatron)) {
            record.addTag(new Tag("public", "accessCopies"));
        }

        List<String> status = record.getStatus();

        // unpublished
        if (status.contains("Unpublished")) {
            record.addTag(new Tag("unpublished"));
        }

        if (status.contains("Deleted")) {
            record.addTag(new Tag("deleted"));
        }

        if (status.contains("Embargoed")) {
            Tag tag = new Tag("embargoed");
            record.addTag(tag);

            Date embargo = acls.getLastActiveEmbargoUntilDate();
            if (embargo != null) {
                tag.addDetail(Long.toString(embargo.getTime()));
            }
        }

        if (accessGroups != null) {
            Set<UserRole> myRoles = acls.getRoles(accessGroups);

            // view only, meaning observer but no editing permissions
            if (myRoles.contains(UserRole.canView)
                    && !acls.hasPermission(accessGroups, Permission.editDescription)) {
                record.addTag(new Tag("view only"));
            }
        }

        if (status.contains("Roles Assigned")) {
            Tag tag = new Tag("roles");

            for (UserRole role : UserRole.values()) {
                List<String> groups = record.getRelation(role.getPredicate());
                if (groups != null) {
                    for (String group : groups) {
                        tag.addDetail(role.getPredicate() + " " + group);
                    }
                }
            }

            record.addTag(tag);
        }
    }
}
