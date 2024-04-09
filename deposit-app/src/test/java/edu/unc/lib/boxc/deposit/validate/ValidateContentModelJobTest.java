package edu.unc.lib.boxc.deposit.validate;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_BASE;
import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.CLOSED;
import static edu.unc.lib.boxc.operations.jms.streaming.StreamingPropertiesRequest.DURACLOUD;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import net.greghaines.jesque.Job;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.auth.api.exceptions.InvalidAssignmentException;
import edu.unc.lib.boxc.auth.fcrepo.services.ContentObjectAccessRestrictionValidator;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.validate.ValidateContentModelJob;
import edu.unc.lib.boxc.deposit.work.JobFailedException;

/**
 *
 * @author bbpennel
 *
 */
public class ValidateContentModelJobTest extends AbstractDepositJobTest {

    private ValidateContentModelJob job;

    private Model model;
    private PID depositPid;
    private PID destPid;

    private Bag depBag;

    @Mock
    private ContentObjectAccessRestrictionValidator aclValidator;
    @Mock
    private RepositoryObjectLoader repoObjectLoader;
    @Mock
    private RepositoryObject destObj;
    @Mock
    private Resource destResc;

    @BeforeEach
    public void init() throws Exception {
        destPid = makePid();

        when(repoObjectLoader.getRepositoryObject(destPid)).thenReturn(destObj);
        Model destModel = ModelFactory.createDefaultModel();
        destResc = destModel.getResource(destPid.getRepositoryPath());
        when(destObj.getResource()).thenReturn(destResc);

        job = new ValidateContentModelJob();
        job.setAclValidator(aclValidator);
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        job.setDepositStatusFactory(depositStatusFactory);
        job.setRepositoryObjectLoader(repoObjectLoader);
        setField(job, "pidMinter", pidMinter);
        setField(job, "depositModelManager", depositModelManager);
        job.init();

        depositPid = job.getDepositPID();
        model = job.getWritableModel();
        depBag = model.createBag(depositPid.getRepositoryPath());

        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.containerId.name(), destPid.getId());
        when(depositStatusFactory.get(depositPid.getId())).thenReturn(depositStatus);
    }

    @Test
    public void folderTest() throws Exception {

        PID folderPid = makePid(CONTENT_BASE);
        Bag folderBag = model.createBag(folderPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);

        depBag.add(folderBag);

        job.closeModel();

        job.run();

        verify(aclValidator).validate(eq(folderBag));
    }

    @Test
    public void folderInvalidEmbargoTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID folderPid = makePid(CONTENT_BASE);
            Bag folderBag = model.createBag(folderPid.getRepositoryPath());
            folderBag.addProperty(RDF.type, Cdr.Folder);
            folderBag.addProperty(CdrAcl.embargoUntil, "forever");

            depBag.add(folderBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void folderInvalidAclsTest() {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            doThrow(new InvalidAssignmentException()).when(aclValidator).validate(any(Resource.class));

            PID folderPid = makePid(CONTENT_BASE);
            Bag folderBag = model.createBag(folderPid.getRepositoryPath());
            folderBag.addProperty(RDF.type, Cdr.Folder);
            folderBag.addProperty(CdrAcl.canDescribe, "user");

            depBag.add(folderBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void workTest() {
        PID objPid = makePid(CONTENT_BASE);
        Bag objBag = model.createBag(objPid.getRepositoryPath());
        objBag.addProperty(RDF.type, Cdr.Work);

        PID childPid = makePid(CONTENT_BASE);
        Resource childResc = model.getResource(childPid.getRepositoryPath());
        childResc.addProperty(RDF.type, Cdr.FileObject);
        Resource origResc = DepositModelHelpers.addDatastream(childResc);
        origResc.addLiteral(CdrDeposit.stagingLocation, "path");
        objBag.add(childResc);

        objBag.addProperty(Cdr.primaryObject, childResc);

        depBag.add(objBag);

        job.closeModel();

        job.run();

        verify(aclValidator).validate(eq(objBag));
        verify(aclValidator).validate(eq(childResc));
    }

    @Test
    public void missingStagingLocationTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID objPid = makePid(CONTENT_BASE);
            Bag objBag = model.createBag(objPid.getRepositoryPath());
            objBag.addProperty(RDF.type, Cdr.Work);

            PID childPid = makePid(CONTENT_BASE);
            Resource childResc = model.getResource(childPid.getRepositoryPath());
            childResc.addProperty(RDF.type, Cdr.FileObject);
            DepositModelHelpers.addDatastream(childResc);
            objBag.add(childResc);

            objBag.addProperty(Cdr.primaryObject, childResc);

            depBag.add(objBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void missingOriginalDatastreamNoStreamingPropertiesTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID objPid = makePid(CONTENT_BASE);
            Bag objBag = model.createBag(objPid.getRepositoryPath());
            objBag.addProperty(RDF.type, Cdr.Work);

            PID childPid = makePid(CONTENT_BASE);
            Resource childResc = model.getResource(childPid.getRepositoryPath());
            childResc.addProperty(RDF.type, Cdr.FileObject);
            objBag.add(childResc);

            objBag.addProperty(Cdr.primaryObject, childResc);

            depBag.add(objBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void noOriginalDatastreamWithStreamingPropertiesTest() {
        PID objPid = makePid(CONTENT_BASE);
        Bag objBag = model.createBag(objPid.getRepositoryPath());
        objBag.addProperty(RDF.type, Cdr.Work);

        PID childPid = makePid(CONTENT_BASE);
        Resource childResc = model.getResource(childPid.getRepositoryPath());
        childResc.addProperty(RDF.type, Cdr.FileObject);
        childResc.addProperty(Cdr.streamingFile, "banjo-playlist.m3u8");
        childResc.addProperty(Cdr.streamingFolder, CLOSED);
        childResc.addProperty(Cdr.streamingHost, DURACLOUD);
        objBag.add(childResc);

        objBag.addProperty(Cdr.primaryObject, childResc);

        depBag.add(objBag);

        job.closeModel();

        job.run();
    }

    @Test
    public void fileObjectOutsideOfWorkTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID childPid = makePid(CONTENT_BASE);
            Resource childResc = model.getResource(childPid.getRepositoryPath());
            childResc.addProperty(RDF.type, Cdr.FileObject);
            Resource origResc = DepositModelHelpers.addDatastream(childResc);
            origResc.addLiteral(CdrDeposit.stagingLocation, "path");

            depBag.add(childResc);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void workInvalidPrimaryObjectTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID objPid = makePid(CONTENT_BASE);
            Bag objBag = model.createBag(objPid.getRepositoryPath());
            objBag.addProperty(RDF.type, Cdr.Work);

            PID childPid = makePid(CONTENT_BASE);
            Resource childResc = model.getResource(childPid.getRepositoryPath());
            childResc.addProperty(RDF.type, Cdr.Folder);
            objBag.add(childResc);

            objBag.addProperty(Cdr.primaryObject, childResc);

            depBag.add(objBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void primaryObjectOnInvalidObjectTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID objPid = makePid(CONTENT_BASE);
            Bag objBag = model.createBag(objPid.getRepositoryPath());
            objBag.addProperty(RDF.type, Cdr.Folder);

            PID childPid = makePid(CONTENT_BASE);
            Resource childResc = model.getResource(childPid.getRepositoryPath());
            childResc.addProperty(RDF.type, Cdr.FileObject);
            Resource origResc = DepositModelHelpers.addDatastream(childResc);
            origResc.addLiteral(CdrDeposit.stagingLocation, "path");
            objBag.add(childResc);

            objBag.addProperty(Cdr.primaryObject, childResc);

            depBag.add(objBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void workValidMemberOrderTest() {
        PID objPid = makePid(CONTENT_BASE);
        Bag objBag = model.createBag(objPid.getRepositoryPath());
        objBag.addProperty(RDF.type, Cdr.Work);

        PID childPid = makePid(CONTENT_BASE);
        Resource childResource = model.getResource(childPid.getRepositoryPath());
        childResource.addProperty(RDF.type, Cdr.FileObject);
        Resource origResource = DepositModelHelpers.addDatastream(childResource);
        origResource.addLiteral(CdrDeposit.stagingLocation, "path");
        objBag.add(childResource);

        PID child2Pid = makePid(CONTENT_BASE);
        Resource child2Resource = model.getResource(child2Pid.getRepositoryPath());
        child2Resource.addProperty(RDF.type, Cdr.FileObject);
        Resource origResc = DepositModelHelpers.addDatastream(child2Resource);
        origResc.addLiteral(CdrDeposit.stagingLocation, "path");
        objBag.add(child2Resource);

        objBag.addProperty(Cdr.memberOrder, childPid.getId() + "|" + child2Pid.getId());

        depBag.add(objBag);

        job.closeModel();

        job.run();
    }

    @Test
    public void workInvalidMemberOrderTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID objPid = makePid(CONTENT_BASE);
            Bag objBag = model.createBag(objPid.getRepositoryPath());
            objBag.addProperty(RDF.type, Cdr.Work);

            PID childPid = makePid(CONTENT_BASE);
            Resource childResource = model.getResource(childPid.getRepositoryPath());
            childResource.addProperty(RDF.type, Cdr.FileObject);
            Resource origResource = DepositModelHelpers.addDatastream(childResource);
            origResource.addLiteral(CdrDeposit.stagingLocation, "path");
            objBag.add(childResource);

            PID randomPid = makePid(CONTENT_BASE);

            objBag.addProperty(Cdr.memberOrder, childPid.getId() + "|" + randomPid.getId());

            depBag.add(objBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void fileObjectBagTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID objPid = makePid(CONTENT_BASE);
            Bag objBag = model.createBag(objPid.getRepositoryPath());
            objBag.addProperty(RDF.type, Cdr.FileObject);

            PID childPid = makePid(CONTENT_BASE);
            Resource childResc = model.getResource(childPid.getRepositoryPath());
            childResc.addProperty(RDF.type, Cdr.FileObject);
            Resource origResc = DepositModelHelpers.addDatastream(childResc);
            origResc.addLiteral(CdrDeposit.stagingLocation, "path");
            objBag.add(childResc);

            depBag.add(objBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void fileObjectToWorkDestinationTest() {
        destResc.addProperty(RDF.type, Cdr.Work);

        PID childPid = makePid(CONTENT_BASE);
        Resource childResc = model.getResource(childPid.getRepositoryPath());
        childResc.addProperty(RDF.type, Cdr.FileObject);
        Resource origResc = DepositModelHelpers.addDatastream(childResc);
        origResc.addLiteral(CdrDeposit.stagingLocation, "path");

        depBag.add(childResc);

        job.closeModel();

        job.run();

        verify(aclValidator).validate(eq(childResc));
    }

    @Test
    public void fileObjectToFolderDestinationTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            destResc.addProperty(RDF.type, Cdr.Folder);

            PID childPid = makePid(CONTENT_BASE);
            Resource childResc = model.getResource(childPid.getRepositoryPath());
            childResc.addProperty(RDF.type, Cdr.FileObject);
            Resource origResc = DepositModelHelpers.addDatastream(childResc);
            origResc.addLiteral(CdrDeposit.stagingLocation, "path");

            depBag.add(childResc);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void objectWithNoTypeTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID objPid = makePid(CONTENT_BASE);
            Bag objBag = model.createBag(objPid.getRepositoryPath());
            objBag.addProperty(CdrDeposit.stagingLocation, "file");

            depBag.add(objBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void objectWithInvalidTypeTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID objPid = makePid(CONTENT_BASE);
            Bag objBag = model.createBag(objPid.getRepositoryPath());
            objBag.addProperty(RDF.type, createResource("http://example.com/invalidType"));

            depBag.add(objBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void invalidChildAclsTest() {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            PID objPid = makePid(CONTENT_BASE);
            Bag objBag = model.createBag(objPid.getRepositoryPath());
            objBag.addProperty(RDF.type, Cdr.Folder);

            PID childPid = makePid(CONTENT_BASE);
            Resource childResc = model.getResource(childPid.getRepositoryPath());
            childResc.addProperty(RDF.type, Cdr.Work);
            childResc.addProperty(CdrAcl.canDescribe, "user");
            objBag.add(childResc);

            doNothing().doThrow(new InvalidAssignmentException()).when(aclValidator).validate(any(Resource.class));

            depBag.add(objBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void invalidChildStreamingPropertiesTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID objPid = makePid(CONTENT_BASE);
            Bag objBag = model.createBag(objPid.getRepositoryPath());
            objBag.addProperty(RDF.type, Cdr.Folder);

            PID childPid = makePid(CONTENT_BASE);
            Resource childResc = model.getResource(childPid.getRepositoryPath());
            childResc.addProperty(RDF.type, Cdr.Work);
            childResc.addProperty(CdrAcl.canDescribe, "user");
            childResc.addProperty(Cdr.streamingHost, DURACLOUD);
            objBag.add(childResc);

            depBag.add(objBag);

            job.closeModel();

            job.run();
        });
    }
}
