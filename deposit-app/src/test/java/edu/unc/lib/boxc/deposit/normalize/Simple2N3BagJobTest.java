package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.normalize.Simple2N3BagJob;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;

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

        job = new Simple2N3BagJob();
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        job.setPremisLoggerFactory(premisLoggerFactory);
        setField(job, "depositModelManager", depositModelManager);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);

        job.init();
    }

    @Test
    public void depositSimple() throws Exception {
        String name = "objectName";
        copyTestPackage("src/test/resources/simpleMods.xml", "data_file.xml", job);

        status.put(DepositField.depositSlug.name(), name);
        URI stagingUri = Paths.get(depositDir.getAbsolutePath(), "data", "data_file.xml").toUri();
        status.put(DepositField.sourceUri.name(), stagingUri.toString());
        status.put(DepositField.fileMimetype.name(), "text/xml");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getURI());
        Resource mainResource = depositBag.iterator().next().asResource();

        assertEquals("Label was not set", mainResource.getProperty(CdrDeposit.label).getString(), name);
        assertTrue("Must have FileObject type", mainResource.hasProperty(RDF.type, Cdr.FileObject));

        Resource originalResc = DepositModelHelpers.getDatastream(mainResource);
        assertEquals(stagingUri.toString(), originalResc.getProperty(CdrDeposit.stagingLocation).getString());
        assertNotNull(originalResc.getProperty(CdrDeposit.size).getLong());
        assertEquals("text/xml", originalResc.getProperty(CdrDeposit.mimetype).getString());

    }

    @Test(expected = JobFailedException.class)
    public void depositSimpleMissingFile() throws Exception {

        status.put(DepositField.depositSlug.name(), "name");
        URI stagingUri = Paths.get(depositDir.getAbsolutePath(), "data_file.xml").toUri();
        status.put(DepositField.sourceUri.name(), stagingUri.toString());

        job.run();

    }
}
