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

import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.Tag;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DateTimeUtil;

public class AccessRestrictionsTagProvider implements TagProvider {
	private static final Logger LOG = LoggerFactory.getLogger(AccessRestrictionsTagProvider.class);
	private static final String[] PUBLIC = new String[] { "public" };
	private static final String EMBARGO = ContentModelHelper.CDRProperty.embargoUntil.getPredicate();
	private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");

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

		// embargo
		for (String rel : record.getRelations()) {
			if (rel.startsWith(EMBARGO)) {
				try {
					// parse the date and compare with now.
					XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendar(
							rel.substring(EMBARGO.length() + 1).trim());
					if (cal.toGregorianCalendar().compareTo(GregorianCalendar.getInstance()) > 0) {
						StringBuilder text = new StringBuilder("This object is embargoed");
						try {
							Date embargoDate = DateTimeUtil.parsePartialUTCToDate(cal.toXMLFormat());
							String embargoString = formatter.print(embargoDate.getTime());
							text.append(" until ").append(embargoString);
						} catch (ParseException e) {
							LOG.debug("Failed to parse date " + cal.toXMLFormat(), e);
						}
						record.addTag(new Tag("embargoed", text.append('.').toString()));
					}
				} catch (DatatypeConfigurationException e) {
					LOG.error("Cannot get DatatypeFactory to parse embargo XML dates.", e);
				}
			}
		}
	}
}
