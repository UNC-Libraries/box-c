package edu.unc.lib.dl.data.ingest.solr.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.common.SolrInputDocument;
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
		
		SetAccessControlFilter filter = new SetAccessControlFilter();
		filter.setAccessControlService(accessControlService);
		
		filter.filter(dip);
		
		assertEquals(1, dip.getDocument().getReadGroup().size());
		assertTrue(dip.getDocument().getReadGroup().contains("public"));
		
		assertNull(dip.getDocument().getAdminGroup());
		
		DocumentObjectBinder binder = new DocumentObjectBinder();
		SolrInputDocument solrDoc = binder.toSolrInputDocument(dip.getDocument());
		
		System.out.println(solrDoc);
	}
}
