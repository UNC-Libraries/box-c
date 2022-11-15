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
package edu.unc.lib.boxc.deposit.validate;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_BASE;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
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

    @Before
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

    @Test(expected = JobFailedException.class)
    public void folderInvalidEmbargoTest() {
        PID folderPid = makePid(CONTENT_BASE);
        Bag folderBag = model.createBag(folderPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);
        folderBag.addProperty(CdrAcl.embargoUntil, "forever");

        depBag.add(folderBag);

        job.closeModel();

        job.run();
    }

    @Test(expected = InvalidAssignmentException.class)
    public void folderInvalidAclsTest() {
        doThrow(new InvalidAssignmentException()).when(aclValidator).validate(any(Resource.class));

        PID folderPid = makePid(CONTENT_BASE);
        Bag folderBag = model.createBag(folderPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);
        folderBag.addProperty(CdrAcl.canDescribe, "user");

        depBag.add(folderBag);

        job.closeModel();

        job.run();
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

    @Test(expected = JobFailedException.class)
    public void missingStagingLocationTest() {
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
    }

    @Test(expected = JobFailedException.class)
    public void missingOriginalDatastreamTest() {
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
    }

    @Test(expected = JobFailedException.class)
    public void fileObjectOutsideOfWorkTest() {

        PID childPid = makePid(CONTENT_BASE);
        Resource childResc = model.getResource(childPid.getRepositoryPath());
        childResc.addProperty(RDF.type, Cdr.FileObject);
        Resource origResc = DepositModelHelpers.addDatastream(childResc);
        origResc.addLiteral(CdrDeposit.stagingLocation, "path");

        depBag.add(childResc);

        job.closeModel();

        job.run();
    }

    @Test(expected = JobFailedException.class)
    public void workInvalidPrimaryObjectTest() {
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
    }

    @Test(expected = JobFailedException.class)
    public void primaryObjectOnInvalidObjectTest() {
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

    @Test(expected = JobFailedException.class)
    public void workInvalidMemberOrderTest() {
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
    }

    @Test(expected = JobFailedException.class)
    public void fileObjectBagTest() {
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

    @Test(expected = JobFailedException.class)
    public void fileObjectToFolderDestinationTest() {
        destResc.addProperty(RDF.type, Cdr.Folder);

        PID childPid = makePid(CONTENT_BASE);
        Resource childResc = model.getResource(childPid.getRepositoryPath());
        childResc.addProperty(RDF.type, Cdr.FileObject);
        Resource origResc = DepositModelHelpers.addDatastream(childResc);
        origResc.addLiteral(CdrDeposit.stagingLocation, "path");

        depBag.add(childResc);

        job.closeModel();

        job.run();
    }

    @Test(expected = JobFailedException.class)
    public void objectWithNoTypeTest() {
        PID objPid = makePid(CONTENT_BASE);
        Bag objBag = model.createBag(objPid.getRepositoryPath());
        objBag.addProperty(CdrDeposit.stagingLocation, "file");

        depBag.add(objBag);

        job.closeModel();

        job.run();
    }

    @Test(expected = JobFailedException.class)
    public void objectWithInvalidTypeTest() {
        PID objPid = makePid(CONTENT_BASE);
        Bag objBag = model.createBag(objPid.getRepositoryPath());
        objBag.addProperty(RDF.type, createResource("http://example.com/invalidType"));

        depBag.add(objBag);

        job.closeModel();

        job.run();
    }

    @Test(expected = InvalidAssignmentException.class)
    public void invalidChildAclsTest() {
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
    }
}
