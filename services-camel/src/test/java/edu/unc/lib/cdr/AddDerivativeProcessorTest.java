package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryChecksum;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.exec.ExecResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.exceptions.base.MockitoException;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.PcdmUse;

public class AddDerivativeProcessorTest {
	private BinaryObject binary;
	private FileObject parent;
	private ExecResult result;
	private AddDerivativeProcessor processor;
	private final String fileName = "small_thumb";
	private final String slug = "small_thumbnail";
	private final String fileExtension = "PNG";
	private int maxRetries = 3;
	private long retryDelay = 2000;
	private File file;

	@Mock
	private Repository repository;

	@Mock
	private Exchange exchange;

	@Mock
	private Message message;
	
	@Before
	public void init() throws Exception {
		initMocks(this);
		processor = new AddDerivativeProcessor(repository, slug, fileExtension, maxRetries, retryDelay);
		file = File.createTempFile(fileName, ".PNG");
		file.deleteOnExit();
		when(exchange.getIn()).thenReturn(message);
		PIDs.setRepository(repository);
		when(repository.getBaseUri()).thenReturn("http://fedora");
	}
	
	@Test
	public void createEnhancementTest() throws Exception {
		try (BufferedWriter writeFile = new BufferedWriter(new FileWriter(file))) {
			writeFile.write("fake image");
		}
		
		String filePath = file.getAbsolutePath().split("\\.")[0];

		when(message.getHeader(eq(FCREPO_URI)))
		.thenReturn("http://fedora/test/original_file");

		when(message.getHeader(eq(CdrBinaryChecksum)))
				.thenReturn("1234");
		
		when(message.getHeader(eq(CdrBinaryPath)))
				.thenReturn(filePath);
		
		when(message.getHeader(eq(CdrBinaryMimeType)))
				.thenReturn("image/png");
		
		binary = mock(BinaryObject.class);
		parent = mock(FileObject.class);
		result = mock(ExecResult.class);
		
		when(repository.getBinary(any(PID.class))).thenReturn(binary);
		when(binary.getParent()).thenReturn(parent);
		when(message.getBody()).thenReturn(result);
		when(result.getStdout()).thenReturn(new ByteArrayInputStream(filePath.getBytes()));
		
		processor.process(exchange);
		
		ArgumentCaptor<InputStream> requestCaptor = ArgumentCaptor.forClass(InputStream.class);
		verify(parent).addDerivative(eq(slug), requestCaptor.capture(), eq(filePath), eq("image/png"), eq(PcdmUse.ThumbnailImage));
	}
	
	@Test
	public void createEnhancementRetryTest() throws Exception {
		try (BufferedWriter writeFile = new BufferedWriter(new FileWriter(file))) {
			writeFile.write("fake image");
		}
		
		String filePath = file.getAbsolutePath().split("\\.")[0];

		when(message.getHeader(eq(FCREPO_URI)))
		.thenReturn("http://fedora/test/original_file");

		when(message.getHeader(eq(CdrBinaryChecksum)))
				.thenReturn("1234");
		
		when(message.getHeader(eq(CdrBinaryPath)))
				.thenReturn(filePath);
		
		when(message.getHeader(eq(CdrBinaryMimeType)))
				.thenReturn("image/png");
		
		binary = mock(BinaryObject.class);
		parent = mock(FileObject.class);
		result = mock(ExecResult.class);
		
		when(repository.getBinary(any(PID.class))).thenReturn(binary);
		when(binary.getParent())
				.thenThrow(new MockitoException("Can't add derivative"))
				.thenReturn(parent);;
		when(message.getBody()).thenReturn(result);
		when(result.getStdout()).thenReturn(new ByteArrayInputStream(filePath.getBytes()));
		
		processor.process(exchange);
		
		ArgumentCaptor<InputStream> requestCaptor = ArgumentCaptor.forClass(InputStream.class);
		
		verify(binary, times(2)).getParent();
		verify(parent).addDerivative(eq(slug), requestCaptor.capture(), eq(filePath), eq("image/png"), eq(PcdmUse.ThumbnailImage));
	}
}
