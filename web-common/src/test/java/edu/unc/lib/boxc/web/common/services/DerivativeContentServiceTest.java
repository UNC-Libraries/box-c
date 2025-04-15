package edu.unc.lib.boxc.web.common.services;

import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public class DerivativeContentServiceTest {
    private AutoCloseable closeable;
    private DerivativeContentService derivativeContentService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private DerivativeService derivativeService;
    @Mock
    private AccessGroupSet principals;
    @Mock
    private HttpServletResponse response;
    ByteArrayOutputStream outputStream;

    @TempDir
    public Path tmpFolder;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);
        derivativeContentService = new DerivativeContentService();
        derivativeContentService.setAccessControlService(accessControlService);
        derivativeContentService.setDerivativeService(derivativeService);
        outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new ServletOutputStreamMock(outputStream));
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testStreamRangeSuccess() throws Exception {
        PID pid = makePid();
        DatastreamType dataType = DatastreamType.AUDIO_ACCESS_COPY;

        // Create a temporary file with some content
        File testFile = createTestFile("m4a");

        // Mock derivative service to return our test file
        initializeDerivative(pid, dataType, testFile);

        // Set range header to request bytes 200-299
        String rangeHeader = "bytes=200-299";

        // Call the method
        derivativeContentService.streamData(pid, dataType.getId(), principals, false, rangeHeader, response);

        // Verify response status and headers
        verify(response).setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        verify(response).setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
        verify(response).setContentLengthLong(100);
        verify(response).setContentType(dataType.getMimetype());
        verify(response).setHeader(HttpHeaders.CONTENT_RANGE, "bytes 200-299/1000");
        verify(response).setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"test.m4a\"");

        // Verify content
        byte[] resultContent = outputStream.toByteArray();
        assertEquals(100, resultContent.length);
        byte[] expectedContent = makeTestContentArray();
        for (int i = 0; i < 100; i++) {
            // Check that the content matches the expected range of the full content
            assertEquals(expectedContent[i + 200], resultContent[i]);
        }
    }

    @Test
    public void testStreamEntireFileSuccess() throws Exception {
        PID pid = makePid();
        DatastreamType dataType = DatastreamType.AUDIO_ACCESS_COPY;

        // Create a temporary file with some content
        File testFile = createTestFile("m4a");

        // Mock derivative service to return our test file
        initializeDerivative(pid, dataType, testFile);

        // Call the method
        derivativeContentService.streamData(pid, dataType.getId(), principals, true, null, response);

        // Verify response status and headers
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentLengthLong(1000);
        verify(response).setContentType(dataType.getMimetype());
        verify(response, never()).setHeader(eq(HttpHeaders.CONTENT_RANGE), anyString());
        verify(response).setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"test.m4a\"");

        // Verify content
        byte[] resultContent = outputStream.toByteArray();
        byte[] expectedContent = makeTestContentArray();
        assertArrayEquals(expectedContent, resultContent);
    }

    @Test
    public void testStreamInvalidDsType() throws Exception {
        PID pid = makePid();
        DatastreamType dataType = DatastreamType.AUDIO_ACCESS_COPY;

        // Create a temporary file with some content
        File testFile = createTestFile("m4a");

        // Mock derivative service to return our test file
        initializeDerivative(pid, dataType, testFile);

        assertThrows(IllegalArgumentException.class, () -> {
            derivativeContentService.streamData(pid, "garbage", principals, true, null, response);
        });
    }

    @Test
    public void testStreamDerivativeDoesNotExist() throws Exception {
        PID pid = makePid();
        DatastreamType dataType = DatastreamType.AUDIO_ACCESS_COPY;

        assertThrows(ResourceNotFoundException.class, () -> {
            derivativeContentService.streamData(pid, dataType.getId(), principals, true, null, response);
        });
    }

    @Test
    public void testInvalidRangeHeader() throws Exception {
        PID pid = makePid();
        DatastreamType dataType = DatastreamType.JP2_ACCESS_COPY;

        File testFile = createTestFile("jp2");

        // Mock derivative service to return our test file
        initializeDerivative(pid, dataType, testFile);

        // Invalid range header
        String rangeHeader = "bytes=invalid-range";

        // Call the method
        derivativeContentService.streamData(pid, dataType.getId(), principals, false, rangeHeader, response);

        // Verify error response
        verify(response).setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        verify(response).setHeader("Content-Range", "bytes */1000");
    }

    @Test
    public void testMultipleRangesNotSupported() throws Exception {
        PID pid = makePid();
        DatastreamType dataType = DatastreamType.AUDIO_ACCESS_COPY;

        File testFile = createTestFile("m4a");

        // Mock derivative service to return our test file
        initializeDerivative(pid, dataType, testFile);

        // Multiple ranges header
        String rangeHeader = "bytes=0-100,200-300";

        // Call the method
        derivativeContentService.streamData(pid, dataType.getId(), principals, false, rangeHeader, response);

        // Verify error response
        verify(response).setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        verify(response).setHeader("Content-Range", "bytes */1000");
    }

    @Test
    public void testEmptyRangeListFallsBackToFullFile() throws Exception {
        PID pid = makePid();
        DatastreamType dataType = DatastreamType.AUDIO_ACCESS_COPY;

        File testFile = createTestFile("jp2");

        // Mock derivative service to return our test file
        initializeDerivative(pid, dataType, testFile);

        // Empty range header that will produce empty list (but is syntactically valid)
        String rangeHeader = "bytes=";

        // Call the method
        derivativeContentService.streamData(pid, dataType.getId(), principals, false, rangeHeader, response);

        // Verify full file headers
        verify(response).setContentLengthLong(1000);

        // Verify content length matches full file
        assertEquals(1000, outputStream.size());
    }

    @Test
    public void testIOExceptionDuringStreaming() throws Exception {
        PID pid = makePid();
        DatastreamType dataType = DatastreamType.AUDIO_ACCESS_COPY;

        File testFile = createTestFile("jp2");

        // Mock derivative service to return our test file
        initializeDerivative(pid, dataType, testFile);

        // Mock HTTP response with output stream that throws exception
        ServletOutputStream errorStream = mock(ServletOutputStream.class);
        doThrow(new IOException("Connection reset")).when(errorStream).write(any(byte[].class), anyInt(), anyInt());
        when(response.getOutputStream()).thenReturn(errorStream);
        when(response.isCommitted()).thenReturn(false);

        // Range header
        String rangeHeader = "bytes=0-199";

        // Call the method
        derivativeContentService.streamData(pid, dataType.getId(), principals, false, rangeHeader, response);

        // Verify error handling
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private byte[] makeTestContentArray() {
        byte[] fileContent = new byte[1000]; // 1KB file
        for (int i = 0; i < fileContent.length; i++) {
            fileContent[i] = (byte) (i % 256);
        }
        return fileContent;
    }

    private File createTestFile(String suffix) throws Exception {
        tmpFolder = tmpFolder.resolve("test." + suffix);
        File testFile = tmpFolder.toFile();
        byte[] fileContent = makeTestContentArray();
        FileUtils.writeByteArrayToFile(testFile, fileContent);
        return testFile;
    }

    private void initializeDerivative(PID pid, DatastreamType dataType, File testFile) {
        var derivative = new DerivativeService.Derivative(dataType, testFile);
        when(derivativeService.getDerivative(pid, dataType)).thenReturn(derivative);
    }

    // Helper class to mock ServletOutputStream
    private static class ServletOutputStreamMock extends ServletOutputStream {
        private final OutputStream outputStream;

        public ServletOutputStreamMock(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            outputStream.write(b, off, len);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // Not implemented for test
        }
    }
}