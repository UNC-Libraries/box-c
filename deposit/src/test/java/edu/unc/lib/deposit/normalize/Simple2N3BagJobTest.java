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
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * @author bbpennel
 * @date Jul 22, 2014
 */
public class Simple2N3BagJobTest extends AbstractNormalizationJobTest {

    private Simple2N3BagJob job;

    private Map<String, String> status;

    @Before
    public void setup() throws Exception {

        status = new HashMap<>();

        when(depositStatusFactory.get(anyString())).thenReturn(status);

        Dataset dataset = TDBFactory.createDataset();

        job = new Simple2N3BagJob();
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        job.setPremisLoggerFactory(premisLoggerFactory);
        setField(job, "dataset", dataset);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);

        job.init();
    }

    @Test
    public void depositSimple() throws Exception {
        String name = "objectName";
        copyTestPackage("src/test/resources/simpleMods.xml", "data_file.xml", job);

        status.put(DepositField.depositSlug.name(), name);
        status.put(DepositField.fileName.name(), "data_file.xml");

        job.run();

        Model model = job.getWritableModel();
        Bag depositBag = model.getBag(job.getDepositPID().getURI());
        Resource mainResource = (Resource) depositBag.iterator().next();

        assertEquals("Folder label was not set", mainResource.getProperty(CdrDeposit.label).getString(), name);

        assertFalse("No RDF types assigned", mainResource.hasProperty(RDF.type));
    }

    @Test(expected = JobFailedException.class)
    public void depositSimpleMissingFile() throws Exception {

        status.put(DepositField.depositSlug.name(), "name");
        status.put(DepositField.fileName.name(), "data_file.xml");

        job.run();

    }
}
