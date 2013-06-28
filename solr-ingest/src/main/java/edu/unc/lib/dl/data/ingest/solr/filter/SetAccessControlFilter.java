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
package edu.unc.lib.dl.data.ingest.solr.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * Filter which sets access control related fields for a document
 * @author bbpennel
 *
 */
public class SetAccessControlFilter extends AbstractIndexDocumentFilter {
	private static final Logger log = LoggerFactory.getLogger(SetAccessControlFilter.class);
	
	private AccessControlService accessControlService;
	
	/**
	 * Requires dip to contain RELS-EXT
	 */
	@Override
	public void filter(DocumentIndexingPackage dip) throws IndexingException {
		Map<String, List<String>> triples = this.retrieveTriples(dip);
		ObjectAccessControlsBean aclBean;
		if (dip.getParentDocument() == null) {
			aclBean = accessControlService.getObjectAccessControls(dip.getPid());
		} else {
			aclBean = new ObjectAccessControlsBean(dip.getParentDocument().getAclBean(), dip.getPid(), triples);
		}
		
		String allowIndexingString = getFirstTripleValue(triples, ContentModelHelper.CDRProperty.allowIndexing.toString());
		boolean allowIndexing = !"no".equals(allowIndexingString);
		
		// If indexing is disallowed then block all patron groups
		if (allowIndexing) {
			// Populate the list of groups with permission to retrieve records based on the view MD perm
			Set<String> readGroups = aclBean.getGroupsByPermission(Permission.viewDescription);
			// Merge in all groups with the list role assigned
			Set<String> listGroups = aclBean.getGroupsByUserRole(UserRole.list); 
			if (listGroups != null) {
				readGroups.addAll(listGroups);
			}
			dip.getDocument().setReadGroup(new ArrayList<String>(readGroups));
		} else {
			dip.getDocument().setReadGroup(new ArrayList<String>(0));
		}
		
		// Populate the list of groups that can view administrative aspects of the object
		Set<String> adminGroups = aclBean.getGroupsByPermission(Permission.viewAdminUI);
		if (adminGroups.size() > 0)
			dip.getDocument().setAdminGroup(new ArrayList<String>(adminGroups));
		
		// Add in flattened role group mappings
		dip.getDocument().setRoleGroup(aclBean.roleGroupsToList());
		if (log.isDebugEnabled())
			log.debug("Role groups: " + dip.getDocument().getRoleGroup());
		dip.setAclBean(aclBean);
	}

	public void setAccessControlService(AccessControlService accessControlService) {
		this.accessControlService = accessControlService;
	}
}
