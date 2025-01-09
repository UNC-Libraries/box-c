package edu.unc.lib.boxc.deposit.impl.model;

import edu.unc.lib.boxc.deposit.api.DepositConstants;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author bbpennel
 */
public class DepositDirectoryManagerTest {
    private static final String OBJECT_ID = "60f617ca-2107-4e7a-bfdb-7243c62fccc2";
    private static final String OBJECT_PATH = "60/f6/17/ca";
    private static final String DEPOSIT_ID = "0f71ea28-b44e-41f4-a7d6-6666da8ad140";
    @TempDir
    public Path tmpFolder;

    private DepositDirectoryManager manager;
    private PID depositPid;
    private PID objectPid;

    @BeforeEach
    void setUp() {
        depositPid = PIDs.get(DEPOSIT_ID);
        objectPid = PIDs.get(OBJECT_ID);
        manager = new DepositDirectoryManager(depositPid, tmpFolder, true, false);
    }

    @Test
    void testGetDepositDir() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID);
        assertEquals(expectedPath, manager.getDepositDir(), "Deposit directory path is incorrect");
    }

    @Test
    void testGetDescriptionDir() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositConstants.DESCRIPTION_DIR);
        assertEquals(expectedPath, manager.getDescriptionDir(), "Description directory path is incorrect");
    }

    @Test
    void testGetHistoryDir() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositConstants.HISTORY_DIR);
        assertEquals(expectedPath, manager.getHistoryDir(), "History directory path is incorrect");
    }

    @Test
    void testGetEventsDir() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositConstants.EVENTS_DIR);
        assertEquals(expectedPath, manager.getEventsDir(), "Events directory path is incorrect");
    }

    @Test
    void testGetAltTextDir() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositConstants.ALT_TEXT_DIR);
        assertEquals(expectedPath, manager.getAltTextDir(), "Alt text directory path is incorrect");
    }

    @Test
    void testGetTechMdDir() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositConstants.TECHMD_DIR);
        assertEquals(expectedPath, manager.getTechMdDir(), "Technical metadata directory path is incorrect");
    }

    @Test
    void testGetDataDir() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositConstants.DATA_DIR);
        assertEquals(expectedPath, manager.getDataDir(), "Data directory path is incorrect");
    }

    @Test
    void testGetModelPath() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositDirectoryManager.MODEL_FILENAME);
        assertEquals(expectedPath, manager.getModelPath(), "Model file path is incorrect");
    }

    @Test
    void testGetModsPath() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositConstants.DESCRIPTION_DIR)
                .resolve(OBJECT_PATH).resolve(OBJECT_ID + ".xml");
        assertEquals(expectedPath, manager.getModsPath(objectPid), "MODS path is incorrect");
    }

    @Test
    void testGetAltTextPath() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositConstants.ALT_TEXT_DIR)
                .resolve(OBJECT_PATH).resolve(OBJECT_ID + ".txt");
        Path resultPath = manager.getAltTextPath(objectPid);
        assertEquals(expectedPath, resultPath, "Alt text path is incorrect");
        assertFalse(Files.exists(resultPath.getParent()), "Alt text directory was not created");
    }

    @Test
    void testGetAltTextCreateDirsPath() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositConstants.ALT_TEXT_DIR)
                .resolve(OBJECT_PATH).resolve(OBJECT_ID + ".txt");
        Path resultPath = manager.getAltTextPath(objectPid, true);
        assertEquals(expectedPath, resultPath, "Alt text path is incorrect");
        assertTrue(Files.exists(resultPath.getParent()), "Alt text directory was not created");
        assertFalse(Files.exists(resultPath), "Alt text file should not be created");
    }

    @Test
    void testGetPremisPath() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositConstants.EVENTS_DIR)
                .resolve(OBJECT_PATH).resolve(OBJECT_ID + ".nt");
        assertEquals(expectedPath, manager.getPremisPath(objectPid), "PREMIS path is incorrect");
    }

    @Test
    void testGetTechMdPath() {
        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositConstants.TECHMD_DIR)
                .resolve(OBJECT_PATH).resolve(OBJECT_ID + ".xml");
        assertEquals(expectedPath, manager.getTechMdPath(objectPid), "Technical metadata path is incorrect");
    }

    @Test
    void testGetHistoryFile() {
        DatastreamType dsType = DatastreamType.MD_DESCRIPTIVE;

        Path expectedPath = tmpFolder.resolve(DEPOSIT_ID).resolve(DepositConstants.HISTORY_DIR)
                .resolve(OBJECT_PATH).resolve(OBJECT_ID + "md_descriptive.xml");
        assertEquals(expectedPath, manager.getHistoryFile(objectPid, dsType), "History file path is incorrect");
    }

    @Test
    void testCreateAndCleanupDirs() {
        manager.createDepositDirectory();
        assertTrue(Files.exists(manager.getDepositDir()), "Deposit directory was not created");
        assertTrue(Files.exists(manager.getDescriptionDir()), "Description directory was not created");
        assertTrue(Files.exists(manager.getHistoryDir()), "History directory was not created");
        assertTrue(Files.exists(manager.getEventsDir()), "Events directory was not created");
        assertTrue(Files.exists(manager.getAltTextDir()), "Alt text directory was not created");
        assertTrue(Files.exists(manager.getTechMdDir()), "Technical metadata directory was not created");
        assertTrue(Files.exists(manager.getDataDir()), "Data directory was not created");

        manager.cleanupDepositDirectory();
        assertFalse(Files.exists(manager.getDepositDir()), "Deposit directory was not deleted");
    }

    @Test
    void testCreateDirsConstructor() {
        manager = new DepositDirectoryManager(depositPid, tmpFolder, true, true);

        assertTrue(Files.exists(manager.getDepositDir()), "Deposit directory was not created");
        assertTrue(Files.exists(manager.getDescriptionDir()), "Description directory was not created");
        assertTrue(Files.exists(manager.getHistoryDir()), "History directory was not created");
        assertTrue(Files.exists(manager.getEventsDir()), "Events directory was not created");
        assertTrue(Files.exists(manager.getAltTextDir()), "Alt text directory was not created");
        assertTrue(Files.exists(manager.getTechMdDir()), "Technical metadata directory was not created");
        assertTrue(Files.exists(manager.getDataDir()), "Data directory was not created");
    }
}
