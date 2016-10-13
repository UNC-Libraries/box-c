package edu.unc.lib.dl.fcrepo4;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;

import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;

public class BinaryObjectTest {
	@Mock
	private Repository mockRepo;
	@Mock
	private RepositoryObjectDataLoader mockLoader;
	@Mock
	private Model mockModel;
	@Mock
	private PID mockPid;
	
	private BinaryObject binObj;
	
	private ByteArrayInputStream stream;
	
	private static final String BASE_PATH = "http://www.myrepo.com";
	private static final String METADATA_PATH = "/fcr:metadata";
	
	@Before
	public void init() throws URISyntaxException {
		MockitoAnnotations.initMocks(this);
		
		byte[] buf = new byte[10];
		stream = new ByteArrayInputStream(buf);
		
		
		when(mockPid.getRepositoryUri()).thenReturn(new URI(BASE_PATH));
		binObj = new BinaryObject(mockPid, mockRepo, mockLoader);
		
		when(mockLoader.loadModel(binObj)).thenReturn(mockLoader);
		
	}

	@Test
	public void testGetMetadataUri() {
		assertEquals(BASE_PATH + METADATA_PATH, binObj.getMetadataUri().toString());
	}

	@Test
	public void testValidateType() {
		// Return the correct RDF types
		 	List<String> types = Arrays.asList(Fcrepo4Repository.Binary.toString());
		 	when(mockLoader.loadTypes(eq(binObj))).thenAnswer(new Answer<RepositoryObjectDataLoader>() {
		        @Override
		 		public RepositoryObjectDataLoader answer(InvocationOnMock invocation) throws Throwable {
		 			binObj.setTypes(types);
		 			return mockLoader;
		 		}
		 	});
		
		 	binObj.validateType();
	}
	
	@Test(expected = ObjectTypeMismatchException.class)
	 public void invalidTypeTest() {
			when(mockLoader.loadTypes(eq(binObj))).thenAnswer(new Answer<RepositoryObjectDataLoader>() {
	 		    @Override
	 		    public RepositoryObjectDataLoader answer(InvocationOnMock invocation) throws Throwable {
	 			    binObj.setTypes(Arrays.asList());
				    return mockLoader;
	 	        }
	        });
	 
	 	binObj.validateType();
	 }

	@Test
	public void testGetBinaryStream() {
		when(mockLoader.getBinaryStream(binObj)).thenReturn(stream);
		assertEquals(binObj.getBinaryStream(), stream);
	}

	@Test
	public void testGetFilename() {
		
		binObj.setFilename("sample.txt");
		assertEquals("sample.txt", binObj.getFilename());
		//TODO: null case
	}


	@Test
	public void testGetMimetype() {
		binObj.setMimetype("text/plain");
		assertEquals("text/plain", binObj.getMimetype());
		//TODO: null case
	}

	@Test
	public void testGetChecksum() {
		binObj.setChecksum("abcd1234");
		assertEquals("abcd1234", binObj.getChecksum());
		//TODO: null case
	}

	@Test
	public void testGetFilesize() {
		long size = 42;
		binObj.setFilesize(size);
		assertTrue(size == binObj.getFilesize());
		//TODO: null case
	}

}
