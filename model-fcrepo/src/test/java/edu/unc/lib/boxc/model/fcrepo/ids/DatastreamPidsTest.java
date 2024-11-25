package edu.unc.lib.boxc.model.fcrepo.ids;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author bbpennel
 */
public class DatastreamPidsTest {
    private PID pid;

    @BeforeEach
    void setUp() {
        pid = TestHelper.makePid();
    }

    @Test
    void testGetDatastreamPid_withMdDescriptiveHistory() {
        PID result = DatastreamPids.getDatastreamPid(pid, DatastreamType.MD_DESCRIPTIVE_HISTORY);
        assertEquals(pid.getId() + "/md/md_descriptive_history", result.getComponentId());
    }

    @Test
    void testGetDatastreamPid_withTechnicalMetadataHistory() {
        PID result = DatastreamPids.getDatastreamPid(pid, DatastreamType.TECHNICAL_METADATA_HISTORY);
        assertEquals(pid.getId() + "/datafs/techmd_fits_history", result.getComponentId());
    }

    @Test
    void testGetDatastreamPid_withOtherType() {
        PID result = DatastreamPids.getDatastreamPid(pid, DatastreamType.MD_DESCRIPTIVE);
        assertEquals(pid.getId() + "/md/md_descriptive", result.getComponentId());
    }

    @Test
    void testGetMdDescriptivePid() {
        PID result = DatastreamPids.getMdDescriptivePid(pid);
        assertEquals(pid.getId() + "/md/md_descriptive", result.getComponentId());
    }

    @Test
    void testGetOriginalFilePid() {
        PID result = DatastreamPids.getOriginalFilePid(pid);
        assertEquals(pid.getId() + "/datafs/original_file", result.getComponentId());
    }

    @Test
    void testGetDepositManifestPid() {
        String manifestName = "manifest_name";
        PID result = DatastreamPids.getDepositManifestPid(pid, manifestName);

        assertEquals(pid.getId() + "/manifest/manifest_name", result.getComponentId());
    }

    @Test
    void testGetDatastreamHistoryPid() {
        PID mdPid = DatastreamPids.getMdDescriptivePid(pid);
        PID result = DatastreamPids.getDatastreamHistoryPid(mdPid);

        assertEquals(pid.getId() + "/md/md_descriptive_history", result.getComponentId());
    }

    @Test
    void testGetAltTextPid() {
        PID result = DatastreamPids.getAltTextPid(pid);
        assertEquals(pid.getId() + "/md/alt_text", result.getComponentId());
        PID history = DatastreamPids.getDatastreamHistoryPid(result);
        assertEquals(pid.getId() + "/md/alt_text_history", history.getComponentId());
    }

    @Test
    void testGetTechnicalMetadataPid() {
        PID result = DatastreamPids.getTechnicalMetadataPid(pid);
        assertEquals(pid.getId() + "/datafs/techmd_fits", result.getComponentId());
        PID history = DatastreamPids.getDatastreamHistoryPid(result);
        assertEquals(pid.getId() + "/datafs/techmd_fits_history", history.getComponentId());
    }

    @Test
    void testGetMdEventsPid() {
        PID result = DatastreamPids.getMdEventsPid(pid);
        assertEquals(pid.getId() + "/md/event_log", result.getComponentId());
    }

    @Test
    void testAccessSurrogatePid() {
        PID result = DatastreamPids.getAccessSurrogatePid(pid);
        assertEquals(pid.getId() + "/datafs/access_surrogate", result.getComponentId());
    }
}
