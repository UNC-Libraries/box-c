package edu.unc.lib.boxc.services.camel.longleaf;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
public class RegisterToLongleafProcessorTest {
    private PIDMinter pidMinter;
    private PID filePid;

    @BeforeEach
    public void setup() {
        pidMinter = new RepositoryPIDMinter();
        filePid = pidMinter.mintContentPid();
    }

    @Test
    public void registerableBinaryOriginal() throws Exception {
        PID binPid = DatastreamPids.getOriginalFilePid(filePid);
        Exchange exchange = createIndividualExchange(binPid);
        assertTrue(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryMdDescriptive() throws Exception {
        PID binPid = DatastreamPids.getMdDescriptivePid(filePid);
        Exchange exchange = createIndividualExchange(binPid);
        assertTrue(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryMdDescriptiveHistory() throws Exception {
        PID binPid = DatastreamPids.getMdDescriptivePid(filePid);
        binPid = DatastreamPids.getDatastreamHistoryPid(binPid);
        Exchange exchange = createIndividualExchange(binPid);
        assertTrue(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryMdEvents() throws Exception {
        PID binPid = DatastreamPids.getMdEventsPid(filePid);
        Exchange exchange = createIndividualExchange(binPid);
        assertTrue(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryTechMd() throws Exception {
        PID binPid = DatastreamPids.getTechnicalMetadataPid(filePid);
        Exchange exchange = createIndividualExchange(binPid);
        assertTrue(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryManifest() throws Exception {
        PID depositPid = pidMinter.mintDepositRecordPid();
        PID binPid = DatastreamPids.getDepositManifestPid(depositPid, "mets.xml");
        Exchange exchange = createIndividualExchange(binPid);
        assertTrue(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryJp2() throws Exception {
        PID binPid = PIDs.get(filePid.getRepositoryPath() + "/data/jp2");
        Exchange exchange = createIndividualExchange(binPid);
        assertFalse(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    @Test
    public void registerableBinaryManifestMetadata() throws Exception {
        PID depositPid = pidMinter.mintDepositRecordPid();
        PID binPid = DatastreamPids.getDepositManifestPid(depositPid, "mets.xml");
        PID mdPid = PIDs.get(binPid.getRepositoryPath() + "/fcr:metadata");
        Exchange exchange = createIndividualExchange(mdPid);
        assertFalse(RegisterToLongleafProcessor.registerableBinary(exchange));
    }

    private Exchange createIndividualExchange(PID pid) throws Exception {
        Exchange exchange = mock(Exchange.class);
        Message msg = mock(Message.class);
        when(exchange.getIn()).thenReturn(msg);
        when(msg.getHeader(FCREPO_URI)).thenReturn(pid.getRepositoryPath());
        return exchange;
    }
}
