package edu.unc.lib.boxc.model.fcrepo.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.FileInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

public class OriginalFileVersionServiceTest {
    private static final String MIMETYPE = "image/tiff";
    private static final String VERSION1_DATE = "20260727195502";
    private static final String VERSION2_DATE = "20260727195530";
    private static final String OBJECT_ID = "e2847b41-e0ee-45bb-bdb3-a97a6241bee5";
    private static final PID OBJECT_PID = PIDs.get(OBJECT_ID);
    private OriginalFileVersionService originalFileVersionService;
    private AutoCloseable closeable;
    private GetBuilder getVersionsBuilder, getVersion1Builder, getVersion2Builder;
    @TempDir
    public Path tmpFolder;
    @Mock
    private FcrepoClient fcrepoClient;
    @Mock
    private FcrepoResponse versionsResponse;
    @Mock
    private FcrepoResponse version1Response;
    @Mock
    private FcrepoResponse version2Response;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        originalFileVersionService = new OriginalFileVersionService();
        originalFileVersionService.setFcrepoClient(fcrepoClient);

        getVersionsBuilder = mock(GetBuilder.class, new SelfReturningAnswer());
        getVersion1Builder = mock(GetBuilder.class, new SelfReturningAnswer());
        getVersion2Builder = mock(GetBuilder.class, new SelfReturningAnswer());

        when(fcrepoClient.get(any(URI.class))).thenAnswer(inv -> {
            URI uri = inv.getArgument(0);
            String uriString = uri.toString();

            if (uriString.endsWith("/fcr:versions")) {
                return getVersionsBuilder;
            }
            if (uriString.contains(VERSION1_DATE)) {
                return getVersion1Builder;
            }
            if (uriString.contains(VERSION2_DATE)) {
                return getVersion2Builder;
            }

            throw new AssertionError("Unexpected URI passed to fcrepoClient.get(): " + uriString);
        });

        when(getVersionsBuilder.perform()).thenReturn(versionsResponse);
        when(versionsResponse.getBody()).thenAnswer(
                inv -> new FileInputStream("src/test/resources/rdf/file-object-versions.rdf"));

        when(getVersion1Builder.perform()).thenReturn(version1Response);
        when(version1Response.getBody()).thenAnswer(
                inv -> new FileInputStream("src/test/resources/rdf/version1.rdf"));

        when(getVersion2Builder.perform()).thenReturn(version2Response);
        when(version2Response.getBody()).thenAnswer(
                inv -> new FileInputStream("src/test/resources/rdf/version2.rdf"));
    }

    @AfterEach
    void closeService() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    public void successTest() {
        Map<String, Map<String, String>> metadataMap = originalFileVersionService.getVersionMetadata(OBJECT_PID);

        assertFalse(metadataMap.isEmpty());
        assertEquals(2, metadataMap.size());

        assertVersionMetadata(metadataMap, VERSION1_DATE, "00276_op0178_0001.tif");
        assertVersionMetadata(metadataMap, VERSION2_DATE, "00276_op0178_0001_2.tif");
    }

    private void assertVersionMetadata(Map<String, Map<String, String>> metadataMap, String date, String expectedFilename) {
        assertTrue(metadataMap.containsKey(date));

        Map<String, String> metadata = metadataMap.get(date);
        assertEquals(expectedFilename, metadata.get("filename"));
        assertEquals(MIMETYPE, metadata.get("mimetype"));
    }
}