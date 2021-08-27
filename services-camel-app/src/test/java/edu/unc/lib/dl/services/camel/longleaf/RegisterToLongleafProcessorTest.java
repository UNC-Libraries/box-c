/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.services.camel.longleaf;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;

/**
 * @author bbpennel
 */
public class RegisterToLongleafProcessorTest {
    private PIDMinter pidMinter;
    private PID filePid;

    @Before
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
