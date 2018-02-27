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
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

public class DirectoryToBagJobTest extends AbstractNormalizationJobTest {

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private DirectoryToBagJob job;

    private Map<String, String> status;

    private File depositDirectory;

    @Before
    public void setup() throws Exception {
        depositDirectory = tmpDir.newFolder("directory-deposit");

        File emptyDir = new File(depositDirectory, "empty_test");
        emptyDir.mkdir();

        File testDirectory = new File(depositDirectory, "test");
        testDirectory.mkdir();

        File testFile = new File(testDirectory, "lorem.txt");
        testFile.createNewFile();

        status = new HashMap<>();

        when(depositStatusFactory.get(anyString())).thenReturn(status);

        Dataset dataset = TDBFactory.createDataset();

        job = new DirectoryToBagJob();
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        job.setPremisLoggerFactory(premisLoggerFactory);
        setField(job, "dataset", dataset);
        setField(job, "depositsDirectory", depositDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);

        job.init();
    }

    @Test
    public void testConversion() throws Exception {
        status.put(DepositField.sourcePath.name(), depositDirectory.getAbsolutePath());
        status.put(DepositField.fileName.name(), "Test File");
        status.put(DepositField.extras.name(), "{\"accessionNumber\" : \"123456\", \"mediaId\" : \"789\"}");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getURI());

        assertEquals(depositBag.size(), 1);

        Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
        assertEquals("Bag folder label was not set", "Test File", bagFolder.getProperty(CdrDeposit.label).getString());
        assertEquals("Content model was not set", RDF.Bag, bagFolder.getPropertyResourceValue(RDF.type));

        Resource emptyFolder = getChildByLabel(bagFolder, "empty_test");
        assertTrue("Content model was not set", emptyFolder.hasProperty(RDF.type, Cdr.Folder));

        Bag emptyBag = model.getBag(emptyFolder.getURI());

        assertEquals(emptyBag.size(), 0);

        Resource folder = getChildByLabel(bagFolder, "test");
        assertTrue("Content model was not set", folder.hasProperty(RDF.type, Cdr.Folder));

        Bag childrenBag = model.getBag(folder.getURI());

        assertEquals(childrenBag.size(), 1);

        // Verify that file and its properties were added to work
        Resource file = getChildByLabel(childrenBag, "lorem.txt");
        assertTrue("Type was not set", file.hasProperty(RDF.type, Cdr.FileObject));

        String tagPath = file.getProperty(CdrDeposit.stagingLocation).getString();
        assertTrue(tagPath.endsWith("directory-deposit/test/lorem.txt"));
    }

    private Resource getChildByLabel(Bag bagResc, String seekLabel) {
        NodeIterator iterator = bagResc.iterator();
        while (iterator.hasNext()) {
            Resource childResc = (Resource) iterator.next();
            String label = childResc.getProperty(CdrDeposit.label).getString();
            if (label.equals(seekLabel)) {
                iterator.close();
                return childResc;
            }
        }
        throw new AssertionError("Failed to find child with label " + seekLabel + " in bag " + bagResc);
    }
}