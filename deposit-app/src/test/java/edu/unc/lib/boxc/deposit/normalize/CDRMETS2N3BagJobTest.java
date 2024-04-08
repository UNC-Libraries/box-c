package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xml.sax.SAXException;

import com.google.common.io.Files;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.mets.METSProfile;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.operations.impl.validation.SchematronValidator;

/**
 *
 * @author harring
 *
 */
public class CDRMETS2N3BagJobTest extends AbstractNormalizationJobTest {
    @Mock
    private Schema metsSipSchema;
    @Mock
    private Validator metsValidator;
    @Mock
    private SchematronValidator schematronValidator;

    private CDRMETS2N3BagJob job;

    private Map<String, String> status;

    private File data;
    private String stagingBaseUri;


    @BeforeEach
    public void setup() throws Exception {
        status = new HashMap<>();

        when(depositStatusFactory.get(anyString())).thenReturn(status);
        when(metsSipSchema.newValidator()).thenReturn(metsValidator);
        makePid(RepositoryPathConstants.CONTENT_BASE);

        data = new File(depositDir, "data");
        data.mkdir();

        Path metsPath = data.toPath().resolve("mets.xml");
        stagingBaseUri = data.toPath().toUri().toString();
        status.put(DepositField.sourceUri.name(), metsPath.toUri().toString());

        job = new CDRMETS2N3BagJob(jobUUID, depositUUID);
        setField(job, "depositModelManager", depositModelManager);
        job.setDepositDirectory(depositDir);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "metsSipSchema", metsSipSchema);
        setField(job, "premisLoggerFactory", premisLoggerFactory);
        setField(job, "pidMinter", pidMinter);
        job.setSchematronValidator(schematronValidator);
        when(schematronValidator.validateReportErrors(any(StreamSource.class), eq(METSProfile.CDR_SIMPLE.name())))
            .thenReturn(new ArrayList<String>());

        when(premisLogger.buildEvent(eq(Premis.Validation))).thenReturn(premisEventBuilder);
        when(premisLogger.buildEvent(eq(Premis.Normalization))).thenReturn(premisEventBuilder);

        job.init();
    }

    @Test
    public void testSimpleDeposit() throws Exception {
        Files.copy(new File("src/test/resources/mets.xml"), new File(data, "mets.xml"));
        job.run();
    }

    @Test
    public void testMissingFile() throws Exception {
        // checks case where no file is provided
        Assertions.assertThrows(JobFailedException.class, () -> job.run());
    }

    @Test
    public void testMETSInvalid() throws Exception {
        Assertions.assertThrows(JobFailedException.class, () -> {
            try {
                doThrow(new SAXException()).when(metsValidator).validate(any(StreamSource.class));
                Files.copy(new File("src/test/resources/mets.xml"), new File(data, "mets.xml"));
                job.run();
            } finally {
                verify(metsValidator).validate(any(StreamSource.class));
            }
        });
    }

    @Test
    public void testPidsAssigned() throws Exception {
        try {
            Files.copy(new File("src/test/resources/mets.xml"), new File(data, "mets.xml"));
            job.run();
        } finally {
            // check that relevant events were created in AbstractMETS and CDRMETS jobs)
            // test case assumes one object belonging to one work in the mets.xml
            verify(premisLogger).buildEvent(eq(Premis.Validation));
            verify(premisLogger, times(4)).buildEvent(eq(Premis.Accession));
            verify(premisEventBuilder, times(5)).addEventDetail(anyString(), any());
            verify(premisEventBuilder, times(4)).addSoftwareAgent(any(PID.class));
            verify(premisEventBuilder, times(5)).write();
        }
    }

    @Test
    public void testObjectAdded() throws Exception {
        Files.copy(new File("src/test/resources/mets.xml"), new File(data, "mets.xml"));
        job.run();
        Model model = job.getReadOnlyModel();
        Bag bag = model.getBag(depositPid.getURI());
        Resource manifestResc = bag.getPropertyResourceValue(CdrDeposit.hasDatastreamManifest);
        assertNotNull(manifestResc);
        assertNotNull(manifestResc.getProperty(CdrDeposit.stagingLocation));
        assertEquals("text/xml", manifestResc.getProperty(CdrDeposit.mimetype).getString());

        NodeIterator childIt = bag.iterator();
        Resource child = (Resource) childIt.next();
        // check that parent is a work and has acl set
        assertTrue(child.hasProperty(RDF.type, Cdr.Work));
        assertEquals(child.getProperty(CdrAcl.embargoUntil).getObject().toString(),
                "2018-01-19T00:00:00^^http://www.w3.org/2001/XMLSchema#dateTime");

        // check that properties get set on child object of work
        Bag childBag = model.getBag(child);
        NodeIterator workIt = childBag.iterator();
        Resource workChild = (Resource) workIt.next();
        Resource originalResc = DepositModelHelpers.getDatastream(workChild);
        assertEquals(stagingBaseUri + "_c19064b2-983f-4b55-90f5-8d4b890055e4",
                originalResc.getProperty(CdrDeposit.stagingLocation).getString());
        assertTrue(originalResc.hasProperty(CdrDeposit.mimetype, "application/pdf"));
        assertTrue(originalResc.hasProperty(CdrDeposit.md5sum, "4cc5eaafcad970174e44c5194b5afab9"));
        assertTrue(originalResc.hasProperty(CdrDeposit.size, "43129"));

        assertTrue(child.hasProperty(Cdr.primaryObject, workChild));
    }

    @Test
    public void testObjectOnlyAdded() throws Exception {
        Files.copy(new File("src/test/resources/mets_object_only.xml"), new File(data, "mets.xml"));
        job.run();
        Model model = job.getReadOnlyModel();
        Bag bag = model.getBag(depositPid.getURI());
        NodeIterator childIt = bag.iterator();
        Resource res = (Resource) childIt.next();
        Resource originalResc = DepositModelHelpers.getDatastream(res);
        assertTrue(res.hasProperty(CdrDeposit.label, "David_Romani_response.pdf"));
        assertTrue(originalResc.hasProperty(CdrDeposit.md5sum, "4cc5eaafcad970174e44c5194b5afab9"));
        assertTrue(originalResc.hasProperty(CdrDeposit.mimetype, "application/pdf"));
        assertTrue(originalResc.hasProperty(CdrDeposit.stagingLocation,
                stagingBaseUri + "_c19064b2-983f-4b55-90f5-8d4b890055e4"));
        assertTrue(originalResc.hasProperty(CdrDeposit.size, "43129"));
    }

}
