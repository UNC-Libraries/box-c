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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.Tag;

public class AccessRestrictionsTagProvider implements TagProvider {
	private static final Logger LOG = LoggerFactory.getLogger(AccessRestrictionsTagProvider.class);
	private static final String[] PUBLIC = new String[] { "public" };

	@Override
	public void addTags(BriefObjectMetadata record, AccessGroupSet accessGroups) {
		// public
		Set<UserRole> publicRoles = record.getAccessControlBean().getRoles(PUBLIC);
		if (publicRoles.contains(UserRole.patron)) {
			record.addTag(new Tag("public", "The public has access to this object."));
		} else if (publicRoles.contains(UserRole.metadataPatron)) {
			record.addTag(new Tag("public", "The public has access to this object's metadata."));
		} else if (publicRoles.contains(UserRole.accessCopiesPatron)) {
			record.addTag(new Tag("public", "This public has access to this object's metadata and access copies."));
		}

		// unpublished
		if (record.getStatus().contains("Unpublished")) {
			record.addTag(new Tag("unpublished", "This object is not published."));
		}
		
		if (record.getStatus().contains("Deleted")) {
			record.addTag(new Tag("deleted", "This object is in the trash and marked for deletion"));
		}
		
		if (record.getStatus().contains("Embargoed")) {
			record.addTag(new Tag("embargoed", "This object is under an active embargo"));
		}

		if (accessGroups != null) {
			Set<UserRole> myRoles = record.getAccessControlBean().getRoles(accessGroups);

			// view only, meaning observer but no editing permissions
			if (myRoles.contains(UserRole.observer)
					&& !record.getAccessControlBean().hasPermission(accessGroups, Permission.editDescription)) {
				record.addTag(new Tag("view only", "You are an observer of this object."));
			}
		}

		if (record.getStatus().contains("Roles Assigned")) {
			record.addTag(new Tag("roles", "This object has roles directly assigned."));
		}
	}
}
