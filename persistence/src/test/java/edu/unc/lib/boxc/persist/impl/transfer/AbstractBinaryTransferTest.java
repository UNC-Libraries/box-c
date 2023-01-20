package edu.unc.lib.boxc.persist.impl.transfer;

import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getOriginalFilePid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.io.TempDir;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * @author bbpennel
 *
 */
public abstract class AbstractBinaryTransferTest {

    protected static final String FILE_CONTENT = "File content";

    @TempDir
    public Path tmpFolder;
    protected Path sourcePath;
    protected Path storagePath;

    protected void createPaths() throws Exception {
        sourcePath = tmpFolder.resolve("source");
        Files.createDirectory(sourcePath);
        storagePath = tmpFolder.resolve("storage");
        Files.createDirectory(storagePath);
    }

    protected Path createSourceFile() throws Exception {
        return createFile(sourcePath.resolve("file.txt"), FILE_CONTENT);
    }

    protected Path createSourceFile(String filename, String content) throws Exception {
        return createFile(sourcePath.resolve(filename), content);
    }

    protected Path createFile(Path path, String content) throws Exception {
        FileUtils.writeStringToFile(path.toFile(), content, "UTF-8");
        return path;
    }

    protected void assertIsSourceFile(Path path) throws Exception {
        assertFileContent(path, FILE_CONTENT);
    }

    protected void assertFileContent(Path path, String content) throws Exception {
        assertTrue(path.toFile().exists(), "File was not present at " + path);
        assertEquals(content, FileUtils.readFileToString(path.toFile(), "UTF-8"));
    }

    protected PID makeBinPid() {
        return getOriginalFilePid(PIDs.get(UUID.randomUUID().toString()));
    }
}
