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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;

public class SetAccessControlFilterTest extends Assert {
//	
//	@Mock
//	private DocumentIndexingPackageDataLoader loader;
//	private DocumentIndexingPackageFactory factory;
//	
//	@Mock
//	private ObjectAccessControlsBean aclBean;
//	
//	@Before
//	public void setup() throws Exception {
//		initMocks(this);
//		
//		factory = new DocumentIndexingPackageFactory();
//		factory.setDataLoader(loader);
//		
//		when(loader.loadAccessControlBean(any(DocumentIndexingPackage.class))).thenReturn(aclBean);
//	}
//
//	@Test
//	public void noAdminGroups() throws Exception {
//		when(aclBean.getGroupsByPermission(eq(Permission.viewDescription)))
//				.thenReturn(new HashSet<>(Arrays.asList("patron")));
//		
//		DocumentIndexingPackage dip = factory.createDip("uuid:item");
//		
//		SetAccessControlFilter filter = new SetAccessControlFilter();
//		filter.filter(dip);
//		
//		assertEquals(1, dip.getDocument().getReadGroup().size());
//		assertTrue(dip.getDocument().getReadGroup().contains("patron"));
//		
//		assertNull(dip.getDocument().getAdminGroup());
//	}
//
//	@Test
//	public void allowIndexingNo() throws Exception {
//		Map<String, List<String>> triples = new HashMap<>();
//		triples.put(CDRProperty.inheritPermissions.toString(), Arrays.asList("false"));
//		triples.put(CDRProperty.allowIndexing.toString(), Arrays.asList("no"));
//		when(loader.loadTriples(any(DocumentIndexingPackage.class))).thenReturn(triples);
//		
//		when(aclBean.getGroupsByPermission(eq(Permission.viewDescription)))
//				.thenReturn(new HashSet<>(Arrays.asList("curator", "patron")));
//		when(aclBean.getGroupsByPermission(eq(Permission.viewAdminUI)))
//				.thenReturn(new HashSet<>(Arrays.asList("curator")));
//		
//		DocumentIndexingPackage dip = factory.createDip("uuid:item");
//		
//		SetAccessControlFilter filter = new SetAccessControlFilter();
//		
//		filter.filter(dip);
//		
//		assertEquals(0, dip.getDocument().getReadGroup().size());
//		
//		assertEquals(1, dip.getDocument().getAdminGroup().size());
//		assertTrue(dip.getDocument().getAdminGroup().contains("curator"));
//	}
//
//	@Test
//	public void unpublishedFromAclBean() throws Exception {
//		when(aclBean.isAncestorsPublished()).thenReturn(true);
//		when(aclBean.getIsPublished()).thenReturn(false);
//		
//		SetAccessControlFilter filter = new SetAccessControlFilter();
//		
//		DocumentIndexingPackage dip = factory.createDip("uuid:item");
//		
//		filter.filter(dip);
//
//		IndexDocumentBean idb = dip.getDocument();
//
//		assertFalse(dip.getIsPublished());
//		assertFalse(idb.getStatus().contains("Published"));
//		assertTrue(idb.getStatus().contains("Unpublished"));
//		assertFalse(idb.getStatus().contains("Parent Unpublished"));
//	}
//	
//	@Test
//	public void unpublishedFromParentAclBean() throws Exception {
//		when(aclBean.isAncestorsPublished()).thenReturn(false);
//		when(aclBean.getIsPublished()).thenReturn(true);
//		when(loader.loadAccessControlBean(any(DocumentIndexingPackage.class))).thenReturn(aclBean);
//		DocumentIndexingPackage parentDip = factory.createDip("uuid:parent");
//		
//		SetAccessControlFilter filter = new SetAccessControlFilter();
//		
//		DocumentIndexingPackage dip = factory.createDip("uuid:item");
//		dip.setParentDocument(parentDip);
//		
//		filter.filter(dip);
//
//		IndexDocumentBean idb = dip.getDocument();
//
//		assertFalse(dip.getIsPublished());
//		assertFalse(idb.getStatus().contains("Published"));
//		assertFalse(idb.getStatus().contains("Unpublished"));
//		assertTrue(idb.getStatus().contains("Parent Unpublished"));
//	}
//	
//	@Test
//	public void publishedFromBothAclBean() throws Exception {
//		Map<String,Collection<String>> roles = new HashMap<String,Collection<String>>();
//		roles.put(UserRole.patron.toString(), Arrays.asList("public"));
//		roles.put(UserRole.curator.toString(), Arrays.asList("curator"));
//		
//		ObjectAccessControlsBean parentAclBean = new ObjectAccessControlsBean(new PID("uuid:parent"), roles, null, new ArrayList<String>(), Arrays.asList("Published"), null);
//		DocumentIndexingPackage parentDip = factory.createDip("uuid:parent");
//		when(loader.loadAccessControlBean(any(DocumentIndexingPackage.class))).thenReturn(parentAclBean);
//		
//		SetAccessControlFilter filter = new SetAccessControlFilter();
//		
//		DocumentIndexingPackage dip = factory.createDip("uuid:item");
//		dip.setParentDocument(parentDip);
//		
//		filter.filter(dip);
//
//		IndexDocumentBean idb = dip.getDocument();
//
//		assertTrue(dip.getIsPublished());
//		assertTrue(idb.getStatus().contains("Published"));
//		assertFalse(idb.getStatus().contains("Unpublished"));
//		assertFalse(idb.getStatus().contains("Parent Unpublished"));
//		assertTrue(idb.getReadGroup().contains("public"));
//		assertTrue(idb.getReadGroup().contains("curator"));
//	}
//	
//	@Test
//	public void embargoedStatus() throws Exception {
//		DocumentIndexingPackage dip = factory.createDip("uuid:item");
//		Map<String, List<String>> triples = new HashMap<>();
//		triples.put(CDRProperty.embargoUntil.toString(), Arrays.asList("2074-02-03T00:00:00"));
//		when(loader.loadTriples(any(DocumentIndexingPackage.class))).thenReturn(triples);
//
//		DocumentIndexingPackage parentCollection = factory.createDip("uuid:collection");
//		parentCollection.setIsPublished(true);
//		dip.setParentDocument(parentCollection);
//
//		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:item"), new HashMap<String, List<String>>(), null, new ArrayList<String>(), null, null);
//		when(loader.loadAccessControlBean(any(DocumentIndexingPackage.class))).thenReturn(aclBean);
//		
//		SetAccessControlFilter filter = new SetAccessControlFilter();
//		filter.filter(dip);
//
//		IndexDocumentBean idb = dip.getDocument();
//
//		assertTrue(dip.getIsPublished());
//		assertTrue(idb.getStatus().contains("Embargoed"));
//	}
//	
//	@Test
//	public void rolesAssigned() throws Exception {
//		Map<String,List<String>> triples = new HashMap<>();
//		triples.put(UserRole.patron.toString(), Arrays.asList("public"));
//		triples.put(UserRole.curator.toString(), Arrays.asList("curator"));
//		triples.put(CDRProperty.inheritPermissions.toString(), Arrays.asList("false"));
//		when(loader.loadTriples(any(DocumentIndexingPackage.class))).thenReturn(triples);
//		
//		DocumentIndexingPackage dip = factory.createDip("uuid:item");
//
//		DocumentIndexingPackage parentCollection = factory.createDip("uuid:collection");
//		parentCollection.setIsPublished(true);
//		dip.setParentDocument(parentCollection);
//
//		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:item"), new HashMap<String, List<String>>(), null, new ArrayList<String>(), null, null);
//		when(loader.loadAccessControlBean(any(DocumentIndexingPackage.class))).thenReturn(aclBean);
//		
//		SetAccessControlFilter filter = new SetAccessControlFilter();
//		filter.filter(dip);
//
//		IndexDocumentBean idb = dip.getDocument();
//
//		assertTrue(dip.getIsPublished());
//		assertTrue(idb.getStatus().contains("Roles Assigned"));
//		assertTrue(idb.getStatus().contains("Not Inheriting Roles"));
//	}
//	
//	@Test
//	public void nonActiveFromAclBean() throws Exception {
//		when(aclBean.getGroupsByPermission(eq(Permission.viewDescription)))
//				.thenReturn(new HashSet<>(Arrays.asList("curator", "patron")));
//		when(aclBean.getGroupsByPermission(eq(Permission.viewAdminUI)))
//				.thenReturn(new HashSet<>(Arrays.asList("curator")));
//		when(aclBean.getIsActive()).thenReturn(false);
//		when(aclBean.getIsPublished()).thenReturn(true);
//		when(aclBean.isAncestorsPublished()).thenReturn(true);
//		
//		//ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:item"), roles, null, new ArrayList<String>(), null, Arrays.asList("Deleted"));
//		//when(loader.loadAccessControlBean(any(DocumentIndexingPackage.class))).thenReturn(aclBean);
//		
//		SetAccessControlFilter filter = new SetAccessControlFilter();
//		DocumentIndexingPackage dip = factory.createDip("uuid:item");
//		filter.filter(dip);
//
//		IndexDocumentBean idb = dip.getDocument();
//
//		assertTrue(dip.getIsDeleted());
//		assertTrue(idb.getStatus().contains("Published"));
//		assertFalse(idb.getReadGroup().contains("public"));
//		assertTrue(idb.getReadGroup().contains("curator"));
//	}
}
