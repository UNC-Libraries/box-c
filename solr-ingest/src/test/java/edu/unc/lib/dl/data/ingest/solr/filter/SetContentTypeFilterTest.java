package edu.unc.lib.dl.data.ingest.solr.filter;

import static org.junit.Assert.*;

import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyListOf;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 * 
 * @author harring
 *
 */
public class SetContentTypeFilterTest {
	
	private static final String PID_STRING = "uuid:07d9594f-310d-4095-ab67-79a1056e7430";
	
	@Mock
	private DocumentIndexingPackageDataLoader loader;
	@Mock
	private DocumentIndexingPackage dip;
	@Mock
	private PID pid;
	@Mock
	private FileObject fileObj;
	@Mock
	private WorkObject workObj;
	@Mock
	private BinaryObject binObj;
	@Mock
	private Resource resource;
	@Mock
	private IndexDocumentBean idb;
	@Captor
	private ArgumentCaptor<List<String>> listCaptor;
	
	private SetContentTypeFilter filter;
	
	@Before
	public void setup() throws Exception {
		idb = new IndexDocumentBean();
		initMocks(this);

		when(pid.getPid()).thenReturn(PID_STRING);
		
		when(dip.getDocument()).thenReturn(idb);
		when(dip.getPid()).thenReturn(pid);
		
		when(workObj.getPrimaryObject()).thenReturn(fileObj);
//		when(dip.getContentObject()).thenReturn(contentObj);
//		
//		when(contentObj.getResource()).thenReturn(resource);
//		
//		when(resource.getPropertyResourceValue(Fcrepo4Repository.created))
//			.thenReturn(ResourceFactory.createResource(DATE_ADDED));
//		when(resource.getPropertyResourceValue(Fcrepo4Repository.lastModified))
//			.thenReturn(ResourceFactory.createResource(DATE_MODIFIED));

		filter = new SetContentTypeFilter();
		//idb = new IndexDocumentBean();
	}

	@Test
	public void testGetContentTypeFromWorkObject() throws Exception {
		when(dip.getContentObject()).thenReturn(workObj);
		when(workObj.getPrimaryObject()).thenReturn(fileObj);
		when(fileObj.getOriginalFile()).thenReturn(binObj);
		when(binObj.getFilename()).thenReturn("primary.xml");
		when(binObj.getMimetype()).thenReturn("text/xml");
		
		filter.filter(dip);
		
		verify(idb).setContentType(listCaptor.capture());
		assertEquals("^text,Text", listCaptor.getValue().get(0));
		assertEquals("/text^xml,xml", listCaptor.getValue().get(1));
	}
	
	@Test
	public void testGetContentTypeFromFileObject() throws Exception {
		when(dip.getContentObject()).thenReturn(fileObj);
		when(fileObj.getOriginalFile()).thenReturn(binObj);
		when(binObj.getFilename()).thenReturn("data.csv");
		when(binObj.getMimetype()).thenReturn("ext.csv");
		
		filter.filter(dip);
		
		verify(idb).setContentType(listCaptor.capture());
		assertEquals("^dataset,Dataset", listCaptor.getValue().get(0));
		assertEquals("/dataset^csv,csv", listCaptor.getValue().get(1));
	}

}
