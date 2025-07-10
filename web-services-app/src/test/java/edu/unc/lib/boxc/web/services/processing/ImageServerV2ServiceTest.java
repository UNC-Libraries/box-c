package edu.unc.lib.boxc.web.services.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import edu.unc.lib.boxc.common.test.TestHelpers;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author bbpennel
 */
public class ImageServerV2ServiceTest {

    private ImageServerV2Service service;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private HttpClientConnectionManager connectionManager;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private StatusLine statusLine;

    @Mock
    private HttpServletResponse servletResponse;

    @Mock
    private HttpEntity httpEntity;

    @Captor
    private ArgumentCaptor<HttpGet> httpGetCaptor;

    private AutoCloseable closeable;

    private ByteArrayOutputStream outputStream;
    private final String TEST_PID = "testpid1";
    private final String IMAGE_SERVER_BASE_PATH = "http://imageserver.example.com/";
    private final String BASE_PATH = "http://repository.example.com/";
    private final String TEST_ENCODED_ID = "te%2Fst%2Fpi%2Fd1%2F";

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        service = new ImageServerV2Service();
        outputStream = new ByteArrayOutputStream();

        // Configure the service
        service.setImageServerProxyBasePath(IMAGE_SERVER_BASE_PATH);
        service.setBasePath(BASE_PATH);

        TestHelpers.setField(service, "httpClient", httpClient);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
        service.shutdown();
    }

    @Test
    public void getMetadata_Success() throws Exception {
        String jsonContent = "{\"@context\":\"http://iiif.io/api/image/2/context.json\"," +
                "\"@id\":\"http://example.org/iiif/test\",\"protocol\":\"http://iiif.io/api/image\"," +
                "\"width\":600,\"height\":400,\"sizes\":[{\"width\":150,\"height\":100},{\"width\":300," +
                "\"height\":200},{\"width\":600,\"height\":400}],\"tiles\":[{\"width\":512," +
                "\"scaleFactors\":[1,2,4,8,16]}],\"profile\":[\"http://iiif.io/api/image/2/level2.json\"," +
                "\"http://iiif.io/api/image/2/level1.json\"]}";

        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        StringEntity stringEntity = new StringEntity(jsonContent, ContentType.APPLICATION_JSON);
        when(httpResponse.getEntity()).thenReturn(stringEntity);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        service.getMetadata(TEST_PID, outputStream, servletResponse);

        verify(httpClient).execute(httpGetCaptor.capture());
        HttpGet httpGet = httpGetCaptor.getValue();
        assertEquals(IMAGE_SERVER_BASE_PATH + TEST_ENCODED_ID + TEST_PID + ".jp2/info.json", httpGet.getURI().toString());

        verify(servletResponse).setHeader("Content-Type", "application/json");
        verify(servletResponse).setHeader("content-disposition", "inline");

        // Verify content was written to output stream
        String outputContent = outputStream.toString();
        // We can't do an exact match because the content is transformed, but we can verify it's not empty
        assert(!outputContent.isEmpty());
    }

    @Test
    public void getMetadata_ClientAbort() throws Exception {
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);

        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenThrow(new IOException("Connection reset"));

        service.getMetadata(TEST_PID, outputStream, servletResponse);

        verify(httpClient).execute(any(HttpGet.class));
    }

    @Test
    public void setConnectionManager() {
        service.setHttpClientConnectionManager(connectionManager);
    }
}
