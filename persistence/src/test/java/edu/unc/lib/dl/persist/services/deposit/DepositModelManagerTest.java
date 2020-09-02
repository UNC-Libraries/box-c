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
package edu.unc.lib.dl.persist.services.deposit;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 */
public class DepositModelManagerTest {

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private DepositModelManager manager;

    private Path tdbPath;

    @Before
    public void setup() throws Exception {
        tdbPath = tmpFolder.newFolder("tdb_data").toPath();
        manager = new DepositModelManager(tdbPath);
    }

    @Test
    public void removeModelCleansUpDataset() throws Exception {
        PID depositPid1 = PIDs.get(UUID.randomUUID().toString());

        Model model1 = manager.getWriteModel(depositPid1);
        Resource resc = model1.getResource("http://example.com/resc1");
        resc.addLiteral(DC.title, "Resc1");
        manager.close(model1);

        assertTdbDirExists(depositPid1);

        manager.removeModel(depositPid1);

        assertTrue(Files.exists(tdbPath));
        assertTdbDirDoesNotExist(depositPid1);
    }

    @Test
    public void cleanupEmptyDatasetsSkipsNonEmpty() throws Exception {
        PID depositPid1 = PIDs.get(UUID.randomUUID().toString());

        Model model1 = manager.getWriteModel(depositPid1);
        Resource resc = model1.getResource("http://example.com/resc1");
        resc.addLiteral(DC.title, "Resc1");
        manager.close(model1);

        assertTdbDirExists(depositPid1);

        manager.cleanupEmptyDatasets();

        assertTrue(Files.exists(tdbPath));
        assertTdbDirExists(depositPid1);
    }

    @Test
    public void cleanupEmptyDatasetsOneEmpty() throws Exception {
        PID depositPid1 = PIDs.get(UUID.randomUUID().toString());

        Model model1 = manager.getWriteModel(depositPid1);
        Resource resc = model1.getResource("http://example.com/resc1");
        resc.addLiteral(DC.title, "Resc1");
        manager.close(model1);

        PID depositPid2 = PIDs.get(UUID.randomUUID().toString());
        Model model2 = manager.getWriteModel(depositPid2);
        Resource resc2 = model2.getResource("http://example.com/resc2");
        resc2.addLiteral(DC.title, "Resc2");
        manager.close(model2);

        // Delete all the stuff inside the second deposit dir
        FileUtils.cleanDirectory(tdbPath.resolve(depositPid2.getId()).toFile());

        assertTdbDirExists(depositPid2);

        manager.cleanupEmptyDatasets();

        assertTrue(Files.exists(tdbPath));
        assertTdbDirExists(depositPid1);
        assertTdbDirDoesNotExist(depositPid2);
    }

    private void assertTdbDirExists(PID depositPid) throws IOException {
        assertTrue("TDB directory did not exist for " + depositPid.getId(),
                Files.list(tdbPath).anyMatch(p -> p.getFileName().toString().equals(depositPid.getId())));
    }

    private void assertTdbDirDoesNotExist(PID depositPid) throws IOException {
        assertTrue("TDB directory did not exist for " + depositPid.getId(),
                Files.list(tdbPath).noneMatch(p -> p.getFileName().toString().equals(depositPid.getId())));
    }
}
