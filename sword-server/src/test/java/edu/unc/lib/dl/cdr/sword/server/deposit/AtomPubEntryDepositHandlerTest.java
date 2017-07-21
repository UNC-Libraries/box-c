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
package edu.unc.lib.dl.cdr.sword.server.deposit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordError;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.PackagingType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class AtomPubEntryDepositHandlerTest {
    @Mock
    private DepositStatusFactory depositStatusFactory;
    @Before
    public void setup() {
        // Initialize mocks created above
        MockitoAnnotations.initMocks(this);
    }
    
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();
    
    @InjectMocks
    @Autowired
    private AtomPubEntryDepositHandler atomPubEntryDepositHandler;

    @Autowired
    private SwordConfigurationImpl swordConfiguration;

    @SuppressWarnings("unchecked")
    @Test
    public void testDoDepositBagit() throws SwordError, IOException {
        File testPayload = tmpDir.newFile("dcDocument.xml");
        FileUtils.copyFile(new File("src/test/resources/dcDocument.xml"), testPayload);
        Deposit d = new Deposit();
        d.setFile(testPayload);
        d.setMd5("ce812d38aec998c6f3a163994b81bb3a");
        d.setFilename("dcDocument.xml");
        d.setMimeType("application/xml");
        d.setSlug("atomPubEntryTest");
        d.setPackaging(PackagingType.ATOM.getUri());
        Parser parser = Abdera.getInstance().getParser();
        FileInputStream in = new FileInputStream("src/test/resources/atompubMODS.xml");
        Document<Entry> doc = parser.<Entry>parse(in);
        d.setEntry(doc.getRoot());
        PID dest = new PID("uuid:destination");
        
        atomPubEntryDepositHandler.doDeposit(dest, d, PackagingType.ATOM, null,
                swordConfiguration, "test-depositor", "test-owner");
                
        verify(depositStatusFactory, atLeastOnce()).save(anyString(), anyMap());
    }

}
