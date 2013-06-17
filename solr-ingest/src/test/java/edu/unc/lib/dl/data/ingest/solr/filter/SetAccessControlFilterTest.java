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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;

import static org.mockito.Mockito.*;

public class SetAccessControlFilterTest extends Assert {

	@Test
	public void noAdminGroups() throws Exception {
		List<String> roleGroupList = Arrays.asList("http://cdr.unc.edu/definitions/roles#patron|unc:app:lib:cdr:patron");
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:item"), roleGroupList);
		
		AccessControlService accessControlService = mock(AccessControlService.class);
		when(accessControlService.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/fileOctetStream.xml")));
		dip.setFoxml(foxml);
		
		SetAccessControlFilter filter = new SetAccessControlFilter();
		filter.setAccessControlService(accessControlService);
		
		filter.filter(dip);
		
		assertEquals(1, dip.getDocument().getReadGroup().size());
		assertTrue(dip.getDocument().getReadGroup().contains("unc:app:lib:cdr:patron"));
		
		assertNull(dip.getDocument().getAdminGroup());
	}
	
	@Test
	public void invalidRole() throws Exception {
		Map<String,Collection<String>> roles = new HashMap<String,Collection<String>>();
		roles.put("http://cdr.unc.edu/definitions/roles#patron", Arrays.asList("public"));
		roles.put("http://cdr.unc.edu/definitions/acl#inheritPermissions", Arrays.asList("false"));
		
		List<String> embargoes = new ArrayList<String>();
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:item"), roles, embargoes);
		
		AccessControlService accessControlService = mock(AccessControlService.class);
		when(accessControlService.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/fileOctetStream.xml")));
		dip.setFoxml(foxml);
		
		SetAccessControlFilter filter = new SetAccessControlFilter();
		filter.setAccessControlService(accessControlService);
		
		filter.filter(dip);
		
		assertEquals(1, dip.getDocument().getReadGroup().size());
		assertTrue(dip.getDocument().getReadGroup().contains("public"));
		
		assertNull(dip.getDocument().getAdminGroup());
	}
	
	@Test
	public void allowIndexingNo() throws Exception {
		Map<String,Collection<String>> roles = new HashMap<String,Collection<String>>();
		roles.put("http://cdr.unc.edu/definitions/roles#patron", Arrays.asList("public"));
		roles.put("http://cdr.unc.edu/definitions/roles#curator", Arrays.asList("curator"));
		roles.put("http://cdr.unc.edu/definitions/acl#inheritPermissions", Arrays.asList("false"));
		
		List<String> embargoes = new ArrayList<String>();
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:item"), roles, embargoes);
		
		AccessControlService accessControlService = mock(AccessControlService.class);
		when(accessControlService.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/allowIndexingNo.xml")));
		dip.setFoxml(foxml);
		
		SetAccessControlFilter filter = new SetAccessControlFilter();
		filter.setAccessControlService(accessControlService);
		
		filter.filter(dip);
		
		assertNull(dip.getDocument().getReadGroup());
		
		assertEquals(1, dip.getDocument().getAdminGroup().size());
		assertTrue(dip.getDocument().getAdminGroup().contains("curator"));
		
		assertTrue(dip.getDocument().getStatus().contains("Not Discoverable"));
		assertFalse(dip.getDocument().getStatus().contains("Not Inheriting Roles"));
		System.out.println(dip.getDocument().getStatus());
	}
	
	@Test
	public void extractEmbargoed() throws Exception {
		Map<String,Collection<String>> roles = new HashMap<String,Collection<String>>();
		roles.put("http://cdr.unc.edu/definitions/roles#patron", Arrays.asList("public"));
		
		List<String> embargoes = new ArrayList<String>();
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:item"), roles, embargoes);
		
		AccessControlService accessControlService = mock(AccessControlService.class);
		when(accessControlService.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/embargoed.xml")));
		dip.setFoxml(foxml);
		
		SetAccessControlFilter filter = new SetAccessControlFilter();
		filter.setAccessControlService(accessControlService);
		
		filter.filter(dip);
		
		assertTrue(dip.getDocument().getStatus().contains("Embargoed"));
	}
	
	@Test
	public void extractRolesAssigned() throws Exception {
		Map<String,Collection<String>> roles = new HashMap<String,Collection<String>>();
		roles.put("http://cdr.unc.edu/definitions/roles#patron", Arrays.asList("public"));
		
		List<String> embargoes = new ArrayList<String>();
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:item"), roles, embargoes);
		
		AccessControlService accessControlService = mock(AccessControlService.class);
		when(accessControlService.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:item");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/rolesAssigned.xml")));
		dip.setFoxml(foxml);
		
		SetAccessControlFilter filter = new SetAccessControlFilter();
		filter.setAccessControlService(accessControlService);
		
		filter.filter(dip);
		
		assertTrue(dip.getDocument().getStatus().contains("Roles Assigned"));
		assertTrue(dip.getDocument().getStatus().contains("Not Inheriting Roles"));
	}
}
