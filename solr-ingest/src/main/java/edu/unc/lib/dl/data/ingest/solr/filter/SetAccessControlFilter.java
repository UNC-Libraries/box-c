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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Filter which sets access control related fields for a document.
 * 
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
		
		// Generate access control information
		ObjectAccessControlsBean aclBean;
		if (dip.getParentDocument() == null || dip.getParentDocument().getAclBean() == null) {
			// No parent object, ask fedora for access control
			aclBean = accessControlService.getObjectAccessControls(dip.getPid());
		} else {
			aclBean = new ObjectAccessControlsBean(dip.getParentDocument().getAclBean(), dip.getPid(), triples);
		}

		List<String> status = new ArrayList<String>();
		setAccessStatus(triples, status);
		setPublicationStatus(dip, aclBean, status);
		setObjectStateStatus(dip, aclBean, status);
		
		dip.getDocument().setStatus(status);

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
			log.debug("Role groups: {}", dip.getDocument().getRoleGroup());
		dip.setAclBean(aclBean);
	}
	
	private void setAccessStatus(Map<String, List<String>> triples, List<String> status) {
		String inheritPermissions = getFirstTripleValue(triples,
				ContentModelHelper.CDRProperty.inheritPermissions.toString());
		if ("false".equals(inheritPermissions)) {
			status.add("Not Inheriting Roles");
		}

		String embargo = getFirstTripleValue(triples, ContentModelHelper.CDRProperty.embargoUntil.toString());
		if (embargo != null) {
			try {
				Date embargoDate = DateTimeUtil.parsePartialUTCToDate(embargo);
				Date currentDate = new Date();
				if (currentDate.before(embargoDate))
					status.add("Embargoed");
			} catch (ParseException e) {
				log.warn("Failed to parse embargo date " + embargo, e);
			}
		}

		String discoverable = getFirstTripleValue(triples, ContentModelHelper.CDRProperty.allowIndexing.toString());
		if (!"yes".equals(discoverable)) {
			status.add("Not Discoverable");
		}

		for (Entry<String, List<String>> tripleEntry : triples.entrySet()) {
			int index = tripleEntry.getKey().indexOf('#');
			if (index > 0) {
				String namespace = tripleEntry.getKey().substring(0, index + 1);
				if (JDOMNamespaceUtil.CDR_ROLE_NS.getURI().equals(namespace)) {
					status.add("Roles Assigned");
					break;
				}
			}
		}
	}

	private void setPublicationStatus(DocumentIndexingPackage dip, ObjectAccessControlsBean aclBean, List<String> status) {
		// Published by default unless overridden by this item or its parents.
		boolean isPublished = aclBean.getIsPublished();
		boolean parentIsPublished = aclBean.isAncestorsPublished();

		// Set the publication status based on this items status and that of its parents.
		if (parentIsPublished) {
			// If the parent is publish, publication status is completely up to the item being processed.
			if (isPublished) {
				status.add("Published");
			} else {
				status.add("Unpublished");
			}
		} else {
			// If the parent is unpublished, then this item is unpublished.
			status.add("Parent Unpublished");
			// Store that this item specifically is unpublished if it is explicitly unpublished
			if (!isPublished) {
				status.add("Unpublished");
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("Parent is published: {}", parentIsPublished);
			log.debug("Item is published: {}", isPublished);
			log.debug("Final Status: {}", status);
		}

		dip.setIsPublished(parentIsPublished && isPublished);
	}

	/**
	 * Inspects an object's Fedora state property to determine status.  Adds "Deleted" state if the object
	 * or any of its ancestors have been marked for deletion.
	 * 
	 * @param dip
	 * @param triples
	 * @param status
	 */
	private void setObjectStateStatus(DocumentIndexingPackage dip, ObjectAccessControlsBean aclBean, List<String> status) {
		// Check if the object itself is tagged as deleted
		if (!aclBean.getIsActive())
			status.add("Deleted");
		
		if (aclBean.isAncestorsActive()) {
			// If no ancestors were deleted either, then this object is active
			if (aclBean.getIsActive())
				status.add("Active");
		} else {
			// At least one ancestor was deleted, so this object is not active
			status.add("Parent Deleted");
		}
		
		// Store computed activity state in the dip for future generations to reference
		dip.setIsDeleted(!aclBean.isActive());
	}

	public void setAccessControlService(AccessControlService accessControlService) {
		this.accessControlService = accessControlService;
	}
}
