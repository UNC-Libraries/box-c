package edu.unc.lib.dl.fcrepo4;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;

import com.hp.hpl.jena.rdf.model.Model;

import edu.unc.lib.dl.fedora.PID;

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
		when(mockLoader.getBinaryStream(binObj)).thenReturn(stream);
		
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
		assertEquals(binObj, binObj.validateType());
	}

	@Test
	public void testGetBinaryStream() {
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
