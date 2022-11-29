package edu.unc.lib.boxc.persist.impl.transfer;

import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getOriginalFilePid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * @author bbpennel
 *
 */
public abstract class AbstractBinaryTransferTest {

    protected static final String FILE_CONTENT = "File content";

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    protected Path sourcePath;
    protected Path storagePath;

    protected void createPaths() throws Exception {
        tmpFolder.create();
        sourcePath = tmpFolder.newFolder("source").toPath();
        storagePath = tmpFolder.newFolder("storage").toPath();
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
        assertTrue("File was not present at " + path, path.toFile().exists());
        assertEquals(content, FileUtils.readFileToString(path.toFile(), "UTF-8"));
    }

    protected PID makeBinPid() {
        return getOriginalFilePid(PIDs.get(UUID.randomUUID().toString()));
    }
}
