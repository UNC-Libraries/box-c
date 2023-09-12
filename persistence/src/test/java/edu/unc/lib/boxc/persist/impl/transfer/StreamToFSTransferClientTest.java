package edu.unc.lib.boxc.persist.impl.transfer;

import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getOriginalFilePid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.transfer.BinaryAlreadyExistsException;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferException;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.impl.storage.HashedFilesystemStorageLocation;

/**
 * @author bbpennel
 *
 */
public class StreamToFSTransferClientTest {

    protected static final String TEST_UUID = "a168cf29-a2a9-4da8-9b8d-025855b180d5";
    protected static final String ORIGINAL_CONTENT = "Some original stuff";
    protected static final String STREAM_CONTENT = "Stream content";
    protected static final String STREAM_CONTENT_SHA1 = "bf29c7fd7f87fe7395b89012e73d91c85a0cb19b";

    protected StreamToFSTransferClient client;
    private AutoCloseable closeable;

    @TempDir
    public Path tmpFolder;
    protected Path sourcePath;
    protected Path storagePath;
    protected StorageLocation storageLoc;

    protected PID binPid;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        sourcePath = tmpFolder.resolve("source");
        Files.createDirectory(sourcePath);
        storagePath = tmpFolder.resolve("storage");
        Files.createDirectory(storagePath);

        HashedFilesystemStorageLocation hashedLoc = new HashedFilesystemStorageLocation();
        hashedLoc.setBase(storagePath.toString());
        hashedLoc.setId("loc1");
        storageLoc = hashedLoc;

        client = new StreamToFSTransferClient(storageLoc);

        binPid = getOriginalFilePid(PIDs.get(TEST_UUID));
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void transfer_NewFile() throws Exception {
        InputStream sourceStream = toStream(STREAM_CONTENT);

        BinaryTransferOutcome outcome = client.transfer(binPid, sourceStream);

        assertContent(outcome, STREAM_CONTENT);
        assertOutcome(outcome, STREAM_CONTENT_SHA1);
    }

    @Test
    public void transfer_ExistingFile() throws Exception {
        Assertions.assertThrows(BinaryAlreadyExistsException.class, () -> {
            // Create existing file content
            Path destPath = createFile();

            // Attempt to transfer new content
            InputStream sourceStream = toStream(STREAM_CONTENT);

            try {
                client.transfer(binPid, sourceStream);
            } finally {
                // Verify that the file was not replaced
                assertContent(destPath, ORIGINAL_CONTENT);
            }
        });
    }

    @Test
    public void transfer_StreamThrowsException() throws Exception {
        Assertions.assertThrows(BinaryTransferException.class, () -> {
            InputStream sourceStream = mock(InputStream.class);
            when(sourceStream.read(any(), anyInt(), anyInt())).thenThrow(new IOException());

            try {
                client.transfer(binPid, sourceStream);
            } finally {
                assertNull(storageLoc.getCurrentStorageUri(binPid));
            }
        });
    }

    @Test
    public void transferReplaceExisting_ExistingFile() throws Exception {
        // Create existing file content
        createFile();

        InputStream sourceStream = toStream(STREAM_CONTENT);

        BinaryTransferOutcome outcome = client.transferReplaceExisting(binPid, sourceStream);
        // Verify that the new file is present
        assertContent(outcome, STREAM_CONTENT);
        assertOutcome(outcome, STREAM_CONTENT_SHA1);
    }

    @Test
    public void transferReplaceExisting_ExistingFile_WriteFails() throws Exception {
        Assertions.assertThrows(BinaryTransferException.class, () -> {
            InputStream sourceStream = mock(InputStream.class);
            when(sourceStream.read(any(), anyInt(), anyInt())).thenThrow(new IOException());

            // Create existing file content
            Path destPath = createFile();

            try {
                client.transferReplaceExisting(binPid, sourceStream);
            } finally {
                // Verify that the content was not replaced
                assertContent(destPath, ORIGINAL_CONTENT);
            }
        });
    }

    @Test
    public void rollbackOnTransferInterruption() throws Exception {
        Path destPath = createFile();
        File destFile = destPath.toFile();
        File parentDir = destPath.getParent().toFile();
        parentDir.setReadOnly();

        InputStream sourceStream = toStream(ORIGINAL_CONTENT);

        try {
            client.transferReplaceExisting(binPid, sourceStream);
        } catch (BinaryTransferException e) {
            assertTrue(destFile.exists(), "Original file should be present");
            assertEquals(1, parentDir.listFiles().length);
        } finally {
            parentDir.setWritable(true);
            destFile.delete();
        }
    }

    @Test
    public void deleteExisting() throws Exception {
        InputStream sourceStream = toStream(STREAM_CONTENT);

        BinaryTransferOutcome outcome = client.transfer(binPid, sourceStream);
        assertTrue(Files.exists(Paths.get(outcome.getDestinationUri())));

        client.delete(outcome.getDestinationUri());
        assertFalse(Files.exists(Paths.get(outcome.getDestinationUri())));
    }

    @Test
    public void deleteNonExistent() throws Exception {
        Assertions.assertThrows(BinaryTransferException.class, () -> {
            URI destUri = storageLoc.getNewStorageUri(binPid);
            client.delete(destUri);
        });
    }

    protected void assertContent(BinaryTransferOutcome outcome, String content) throws Exception {
        assertContent(Paths.get(outcome.getDestinationUri()), content);
    }

    protected void assertContent(Path path, String content) throws Exception {
        assertTrue(path.toFile().exists(), "Source content was not present at " + path);
        assertEquals(content, FileUtils.readFileToString(path.toFile(), "UTF-8"));
    }

    protected InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    private Path createFile() throws Exception {
        Path path = Paths.get(storageLoc.getNewStorageUri(binPid));
        FileUtils.copyInputStreamToFile(toStream(ORIGINAL_CONTENT), path.toFile());
        return path;
    }

    protected void assertOutcome(BinaryTransferOutcome outcome, String expectedSha1) {
        assertNotNull(outcome, "Outcome was not returned");
        assertTrue(Paths.get(outcome.getDestinationUri()).startsWith(storagePath),
                "Destination file must be in the destination storage location");
        assertEquals(expectedSha1, outcome.getSha1(), "Unexpected outcome digest");
    }
}
