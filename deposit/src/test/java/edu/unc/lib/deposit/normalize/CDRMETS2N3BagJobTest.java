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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.xml.sax.SAXException;

import com.google.common.io.Files;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.xml.METSProfile;

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


    @Before
    public void setup() throws Exception {
        status = new HashMap<>();
        status.put(DepositField.fileName.name(), "src/test/resources/mets.xml");

        when(depositStatusFactory.get(anyString())).thenReturn(status);
        when(metsSipSchema.newValidator()).thenReturn(metsValidator);
        Dataset dataset = TDBFactory.createDataset();
        makePid(RepositoryPathConstants.CONTENT_BASE);

        data = new File(depositDir, "data");
        data.mkdir();

        job = new CDRMETS2N3BagJob(jobUUID, depositUUID);
        setField(job, "dataset", dataset);
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

    @Test(expected = JobFailedException.class)
    public void testMissingFile() throws Exception {
        // checks case where no file is provided
        job.run();
    }

    @Test(expected = JobFailedException.class)
    public void testMETSInvalid() throws Exception {
        try {
            doThrow(new SAXException()).when(metsValidator).validate(any(StreamSource.class));
            Files.copy(new File("src/test/resources/mets.xml"), new File(data, "mets.xml"));
            job.run();
        } finally {
            verify(metsValidator).validate(any(StreamSource.class));
        }
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
            verify(premisLogger, times(4)).buildEvent(eq(Premis.Normalization));
            verify(premisEventBuilder, times(5)).addEventDetail(anyString(), Matchers.<Object>anyVararg());
            verify(premisEventBuilder, times(4)).addSoftwareAgent(anyString());
            verify(premisEventBuilder, times(5)).create();
        }
    }

    @Test
    public void testObjectAdded() throws Exception {
        Files.copy(new File("src/test/resources/mets.xml"), new File(data, "mets.xml"));
        job.run();
        Model model = job.getReadOnlyModel();
        Bag bag = model.getBag(depositPid.getURI());
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
        assertTrue(workChild.hasProperty(CdrDeposit.stagingLocation,
                "data/_c19064b2-983f-4b55-90f5-8d4b890055e4"));
        assertTrue(workChild.hasProperty(CdrDeposit.mimetype, "application/pdf"));
        assertTrue(workChild.hasProperty(CdrDeposit.md5sum, "4cc5eaafcad970174e44c5194b5afab9"));
        assertTrue(workChild.hasProperty(CdrDeposit.size, "43129"));
    }

    @Test
    public void testObjectOnlyAdded() throws Exception {
        Files.copy(new File("src/test/resources/mets_object_only.xml"), new File(data, "mets.xml"));
        job.run();
        Model model = job.getReadOnlyModel();
        Bag bag = model.getBag(depositPid.getURI());
        NodeIterator childIt = bag.iterator();
        Resource res = (Resource) childIt.next();
        assertTrue(res.hasProperty(CdrDeposit.label, "David_Romani_response.pdf"));
        assertTrue(res.hasProperty(CdrDeposit.md5sum, "4cc5eaafcad970174e44c5194b5afab9"));
        assertTrue(res.hasProperty(CdrDeposit.mimetype, "application/pdf"));
        assertTrue(res.hasProperty(CdrDeposit.stagingLocation,
                "data/_c19064b2-983f-4b55-90f5-8d4b890055e4"));
        assertTrue(res.hasProperty(CdrDeposit.size, "43129"));
    }

}
