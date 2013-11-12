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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.Tag;
import static org.mockito.Mockito.*;

public class AccessRestrictionsTagProviderTest extends Assert {

	@Test
	public void embargoed() {
		AccessRestrictionsTagProvider tagProvider = new AccessRestrictionsTagProvider();
		BriefObjectMetadata metadata = mock(BriefObjectMetadata.class);
		Set<UserRole> roles = new HashSet<UserRole>();
		ObjectAccessControlsBean access = mock(ObjectAccessControlsBean.class);
		when(access.getRoles(any(String[].class))).thenReturn(roles);
		when(metadata.getAccessControlBean()).thenReturn(access);
		when(metadata.getRelations()).thenReturn(Arrays.asList("embargo-until|2084-03-05T00:00:00"));

		tagProvider.addTags(metadata, null);

		Mockito.verify(metadata, Mockito.atMost(1)).addTag(any(Tag.class));
	}

	@Test
	public void viewOnly() {
		AccessRestrictionsTagProvider tagProvider = new AccessRestrictionsTagProvider();
		BriefObjectMetadataBean metadata = new BriefObjectMetadataBean();
		metadata.setStatus(new ArrayList<String>());
		metadata.setRelations(new ArrayList<String>());
		List<String> roleGroupList = Arrays.asList("http://cdr.unc.edu/definitions/roles#observer|obs");

		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("test"), roleGroupList);
		metadata.setAccessControlBean(aclBean);

		AccessGroupSet groups = new AccessGroupSet("obs");

		tagProvider.addTags(metadata, groups);

		assertEquals("view only", metadata.getTags().get(0).getLabel());
	}

	@Test
	public void overrideObserverTagWithHigherGrant() {
		AccessRestrictionsTagProvider tagProvider = new AccessRestrictionsTagProvider();
		BriefObjectMetadataBean metadata = new BriefObjectMetadataBean();
		metadata.setStatus(new ArrayList<String>());
		metadata.setRelations(new ArrayList<String>());
		List<String> roleGroupList = Arrays.asList("http://cdr.unc.edu/definitions/roles#observer|obs",
				"http://cdr.unc.edu/definitions/roles#curator|obs");

		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("test"), roleGroupList);
		metadata.setAccessControlBean(aclBean);

		AccessGroupSet groups = new AccessGroupSet("obs");

		tagProvider.addTags(metadata, groups);

		// No tags were added, so tag array never initialized
		assertNull(metadata.getTags());
	}
	
	@Test
	public void deleted() {
		AccessRestrictionsTagProvider tagProvider = new AccessRestrictionsTagProvider();
		BriefObjectMetadata metadata = mock(BriefObjectMetadata.class);
		Set<UserRole> roles = new HashSet<UserRole>();
		ObjectAccessControlsBean access = mock(ObjectAccessControlsBean.class);
		when(access.getRoles(any(String[].class))).thenReturn(roles);
		when(metadata.getAccessControlBean()).thenReturn(access);
		when(metadata.getStatus()).thenReturn(Arrays.asList("Deleted"));
		
		tagProvider.addTags(metadata, null);
		
		Mockito.verify(metadata, Mockito.atMost(1)).addTag(any(Tag.class));
	}
}
