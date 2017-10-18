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
package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

public class BagIt2N3BagJobTest extends AbstractNormalizationJobTest {

    private BagIt2N3BagJob job;

    private Map<String, String> status;

    @Captor
    private ArgumentCaptor<String> filePathCaptor;

    @Before
    public void setup() throws Exception {
        status = new HashMap<>();
        when(depositStatusFactory.get(anyString())).thenReturn(status);
        Dataset dataset = TDBFactory.createDataset();

        job = new BagIt2N3BagJob();
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "dataset", dataset);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        job.init();
    }

    @Test
    public void testConversion() throws Exception {
        String sourcePath = "src/test/resources/paths/valid-bag";
        status.put(DepositField.sourcePath.name(), sourcePath);
        status.put(DepositField.fileName.name(), "Test File");
        status.put(DepositField.extras.name(), "{\"accessionNumber\" : \"123456\", \"mediaId\" : \"789\"}");
        String absoluteSourcePath = "file://" + Paths.get(sourcePath).toAbsolutePath().toString();

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getURI());

        assertEquals(depositBag.size(), 1);

        Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
        assertEquals("Bag folder label was not set", "Test File", bagFolder.getProperty(CdrDeposit.label).getString());
        assertTrue("Missing RDF type", bagFolder.hasProperty(RDF.type, Cdr.Folder ));

        Resource folder = (Resource) bagFolder.iterator().next();

        assertEquals("Folder label was not set", folder.getProperty(CdrDeposit.label).getString(), "test");
        assertTrue("Missing RDF type", folder.hasProperty(RDF.type, Cdr.Folder));

        Bag childrenBag = model.getBag(folder.getURI());

        assertEquals(childrenBag.size(), 2);

        // Put children into a map since we can't guarantee order from jena
        Map<String, Resource> children = new HashMap<>(2);
        NodeIterator childIt = childrenBag.iterator();
        while (childIt.hasNext()) {
            Resource file = (Resource) childIt.next();
            children.put(file.getProperty(CdrDeposit.label).getString(), file);
        }

        // Verify that all manifests were added.
        verify(depositStatusFactory, times(2)).addManifest(anyString(), filePathCaptor.capture());
        List<String> capturedFilePaths = Arrays.asList(absoluteSourcePath + "/bagit.txt",
                absoluteSourcePath + "/manifest-md5.txt");
        assertEquals(capturedFilePaths, filePathCaptor.getAllValues());

        // Verify that files and their properties were added
        assertFileAdded(children.get("lorem.txt"), "fa5c89f3c88b81bfd5e821b0316569af",
                absoluteSourcePath + "/data/test/lorem.txt");

        assertFileAdded(children.get("ipsum.txt"), "e78f5438b48b39bcbdea61b73679449d",
                absoluteSourcePath + "/data/test/ipsum.txt");

        // Verify that the description file for the bag exists
        File modsFile = new File(job.getDescriptionDir(), PIDs.get(bagFolder.getURI()).getUUID() + ".xml");
        assertTrue(modsFile.exists());

        Set<String> cleanupSet = new HashSet<>();
        StmtIterator it = depositBag.listProperties(CdrDeposit.cleanupLocation);
        while (it.hasNext()) {
            Statement stmt = it.nextStatement();
            cleanupSet.add(stmt.getString());
        }

        assertEquals("Incorrect number of objects identified for cleanup", 3, cleanupSet.size());
        assertTrue("Cleanup of bag not set", cleanupSet.contains(absoluteSourcePath + "/"));
        assertTrue("Cleanup of manifest not set", cleanupSet.contains(capturedFilePaths.get(0)));
        assertTrue("Cleanup of manifest not set", cleanupSet.contains(capturedFilePaths.get(1)));
    }

    private void assertFileAdded(Resource file, String md5sum, String fileLocation) {
        assertTrue("Missing RDF type", file.hasProperty(RDF.type, Cdr.FileObject));
        assertEquals("Checksum was not set", md5sum,
                file.getProperty(CdrDeposit.md5sum).getString());
        assertEquals("File location not set", fileLocation,
                file.getProperty(CdrDeposit.stagingLocation).getString());
    }

    @Test(expected = JobFailedException.class)
    public void testInvalid() throws Exception {
        status.put(DepositField.sourcePath.name(), "src/test/resources/paths/invalid-bag");

        job.run();
    }

}
