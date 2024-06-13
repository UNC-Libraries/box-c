package edu.unc.lib.boxc.deposit.validate;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.fcrepo4.AbstractFedoraDepositJobIT;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.validate.ValidateDestinationJob;
import edu.unc.lib.boxc.deposit.work.JobFailedException;

/**
 * @author bbpennel
 */
public class ValidateDestinationJobIT extends AbstractFedoraDepositJobIT {

    private static final String ADMIN_PRINC = "adminGroup";
    private static final String GLOBAL_INGESTOR_PRINC = "ingestGroup";
    private static final String DEPOSITOR_NAME = "boxy_depositor";

    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private DepositStatusFactory depositStatusFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private RepositoryObjectTreeIndexer treeIndexer;

    private ValidateDestinationJob job;

    private Model depModel;
    private Bag depBag;

    @BeforeEach
    public void init() throws Exception {
        job = new ValidateDestinationJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        setField(job, "aclService", aclService);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "repoObjLoader", repoObjLoader);
        setField(job, "depositModelManager", depositModelManager);

        depModel = job.getWritableModel();
        depBag = depModel.createBag(depositPid.getRepositoryPath());
    }

    @AfterEach
    public void tearDown() {
        job.closeModel();
    }

    @Test
    public void rootObject_AddAdminUnit() throws Exception {
        treeIndexer.indexAll(baseAddress);

        setDestinationAndPermissions(rootObj.getPid(), DEPOSITOR_NAME, ADMIN_PRINC);

        addChildContainer(depBag, Cdr.AdminUnit, "Boxc Unit");

        job.closeModel();

        job.run();
    }

    @Test
    public void rootObject_AddInvalidTypes() throws Exception {
        treeIndexer.indexAll(baseAddress);

        setDestinationAndPermissions(rootObj.getPid(), DEPOSITOR_NAME, ADMIN_PRINC);

        Resource child1 = addChildContainer(depBag, Cdr.ContentRoot, "Extra Content Root");
        Resource child2 = addChildContainer(depBag, Cdr.Collection, "Stray Unit");
        Resource child3 = addChildContainer(depBag, Cdr.Folder, "Stray Folder");
        Resource child4 = addChildContainer(depBag, Cdr.Work, "Stray Work");
        Resource child5 = addChildFile(depBag, "File1", true);
        Resource child6 = addChildFile(depBag, "File2", false);

        job.closeModel();

        try {
            job.run();
            fail();
        } catch (JobFailedException e) {
            assertChildrenFailed(e, child1, child2, child3, child4, child5, child6);
        }
    }

    private void assertChildrenFailed(JobFailedException e, Resource... childRescs) {
        String details = e.getDetails();
        Model model = job.getReadOnlyModel();
        for (Resource childResc: childRescs) {
            childResc = model.getResource(childResc.getURI());
            String childId = PIDs.get(childResc.getURI()).getId();
            assertTrue(details.contains(childId), "Expected child " + childId + " with types " +
                    childResc.listProperties(RDF.type).toList() + " to fail validation, but it passed");
        }

        assertTrue(details.contains(childRescs.length +  " object(s) for deposit do not meet this requirement"));
    }

    @Test
    public void rootObject_AddAdminUnit_InsufficientPermissions() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            treeIndexer.indexAll(baseAddress);

            setDestinationAndPermissions(rootObj.getPid(), DEPOSITOR_NAME, GLOBAL_INGESTOR_PRINC);

            addChildContainer(depBag, Cdr.AdminUnit, "Boxc Unit");

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void unit_AddCollection() throws Exception {
        AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
        rootObj.addMember(unitObj);

        treeIndexer.indexAll(baseAddress);

        setDestinationAndPermissions(unitObj.getPid(), DEPOSITOR_NAME, ADMIN_PRINC);

        addChildContainer(depBag, Cdr.Collection, "New Collection");

        job.closeModel();

        job.run();
    }

    @Test
    public void unit_AddInvalidTypes() throws Exception {
        AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
        rootObj.addMember(unitObj);

        treeIndexer.indexAll(baseAddress);

        setDestinationAndPermissions(unitObj.getPid(), DEPOSITOR_NAME, ADMIN_PRINC);

        Resource child1 = addChildContainer(depBag, Cdr.ContentRoot, "Extra Content Root");
        Resource child2 = addChildContainer(depBag, Cdr.AdminUnit, "Stray Unit");
        Resource child3 = addChildContainer(depBag, Cdr.Folder, "Stray Folder");
        Resource child4 = addChildContainer(depBag, Cdr.Work, "Stray Work");
        Resource child5 = addChildFile(depBag, "File1", true);
        Resource child6 = addChildFile(depBag, "File2", false);

        job.closeModel();

        try {
            job.run();
            fail();
        } catch (JobFailedException e) {
            assertChildrenFailed(e, child1, child2, child3, child4, child5, child6);
        }
    }

    @Test
    public void unit_AddCollection_InsufficientPermssions() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
            rootObj.addMember(unitObj);

            treeIndexer.indexAll(baseAddress);

            setDestinationAndPermissions(unitObj.getPid(), DEPOSITOR_NAME, GLOBAL_INGESTOR_PRINC);

            addChildContainer(depBag, Cdr.Collection, "New Collection");

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void collection_AddValidTypes() throws Exception {
        AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
        rootObj.addMember(unitObj);
        CollectionObject collObj = repoObjFactory.createCollectionObject(null);
        unitObj.addMember(collObj);

        treeIndexer.indexAll(baseAddress);

        setDestinationAndPermissions(collObj.getPid(), DEPOSITOR_NAME, GLOBAL_INGESTOR_PRINC);

        addChildContainer(depBag, Cdr.Folder, "New Folder");
        addChildContainer(depBag, Cdr.Work, "New Work");

        job.closeModel();

        job.run();
    }

    @Test
    public void collection_AddInvalidTypes() throws Exception {
        AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
        rootObj.addMember(unitObj);
        CollectionObject collObj = repoObjFactory.createCollectionObject(null);
        unitObj.addMember(collObj);

        treeIndexer.indexAll(baseAddress);

        setDestinationAndPermissions(collObj.getPid(), DEPOSITOR_NAME, GLOBAL_INGESTOR_PRINC);

        Resource child1 = addChildContainer(depBag, Cdr.ContentRoot, "Extra Content Root");
        Resource child2 = addChildContainer(depBag, Cdr.AdminUnit, "Stray Unit");
        Resource child3 = addChildContainer(depBag, Cdr.Collection, "Stray Collection");
        Resource child4 = addChildFile(depBag, "File1", true);
        Resource child5 = addChildFile(depBag, "File2", false);

        job.closeModel();

        try {
            job.run();
            fail();
        } catch (JobFailedException e) {
            assertChildrenFailed(e, child1, child2, child3, child4, child5);
        }
    }

    @Test
    public void collect_AddFolder_InsufficientPermssions() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
            rootObj.addMember(unitObj);
            CollectionObject collObj = repoObjFactory.createCollectionObject(null);
            unitObj.addMember(collObj);

            treeIndexer.indexAll(baseAddress);

            setDestinationAndPermissions(collObj.getPid(), DEPOSITOR_NAME, "some_group");

            addChildContainer(depBag, Cdr.Folder, "New Folder");

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void folder_AddValidTypes() throws Exception {
        AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
        rootObj.addMember(unitObj);
        CollectionObject collObj = repoObjFactory.createCollectionObject(null);
        unitObj.addMember(collObj);
        FolderObject folderObj = repoObjFactory.createFolderObject(null);
        collObj.addMember(folderObj);

        treeIndexer.indexAll(baseAddress);

        setDestinationAndPermissions(folderObj.getPid(), DEPOSITOR_NAME, GLOBAL_INGESTOR_PRINC);

        addChildContainer(depBag, Cdr.Folder, "New Folder");
        addChildContainer(depBag, Cdr.Work, "New Work");

        job.closeModel();

        job.run();
    }

    @Test
    public void folder_AddInvalidTypes() throws Exception {
        AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
        rootObj.addMember(unitObj);
        CollectionObject collObj = repoObjFactory.createCollectionObject(null);
        unitObj.addMember(collObj);
        FolderObject folderObj = repoObjFactory.createFolderObject(null);
        collObj.addMember(folderObj);

        treeIndexer.indexAll(baseAddress);

        setDestinationAndPermissions(folderObj.getPid(), DEPOSITOR_NAME, GLOBAL_INGESTOR_PRINC);

        Resource child1 = addChildContainer(depBag, Cdr.ContentRoot, "Extra Content Root");
        Resource child2 = addChildContainer(depBag, Cdr.AdminUnit, "Stray Unit");
        Resource child3 = addChildContainer(depBag, Cdr.Collection, "Stray Collection");
        Resource child4 = addChildFile(depBag, "File1", true);
        Resource child5 = addChildFile(depBag, "File2", false);

        job.closeModel();

        try {
            job.run();
            fail();
        } catch (JobFailedException e) {
            assertChildrenFailed(e, child1, child2, child3, child4, child5);
        }
    }

    @Test
    public void folder_AddFolder_InsufficientPermssions() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
            rootObj.addMember(unitObj);
            CollectionObject collObj = repoObjFactory.createCollectionObject(null);
            unitObj.addMember(collObj);
            FolderObject folderObj = repoObjFactory.createFolderObject(null);
            collObj.addMember(folderObj);

            treeIndexer.indexAll(baseAddress);

            setDestinationAndPermissions(folderObj.getPid(), DEPOSITOR_NAME, "some_group");

            addChildContainer(depBag, Cdr.Folder, "New Folder");

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void work_AddFile() throws Exception {
        AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
        rootObj.addMember(unitObj);
        CollectionObject collObj = repoObjFactory.createCollectionObject(null);
        unitObj.addMember(collObj);
        FolderObject folderObj = repoObjFactory.createFolderObject(null);
        collObj.addMember(folderObj);
        WorkObject workObj = repoObjFactory.createWorkObject(null);
        folderObj.addMember(workObj);

        treeIndexer.indexAll(baseAddress);

        setDestinationAndPermissions(workObj.getPid(), DEPOSITOR_NAME, GLOBAL_INGESTOR_PRINC);

        addChildFile(depBag, "File1", true);
        addChildFile(depBag, "File2", false);

        job.closeModel();

        job.run();
    }

    @Test
    public void work_AddInvalidTypes() throws Exception {
        AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
        rootObj.addMember(unitObj);
        CollectionObject collObj = repoObjFactory.createCollectionObject(null);
        unitObj.addMember(collObj);
        FolderObject folderObj = repoObjFactory.createFolderObject(null);
        collObj.addMember(folderObj);
        WorkObject workObj = repoObjFactory.createWorkObject(null);
        folderObj.addMember(workObj);

        treeIndexer.indexAll(baseAddress);

        setDestinationAndPermissions(workObj.getPid(), DEPOSITOR_NAME, GLOBAL_INGESTOR_PRINC);

        Resource child1 = addChildContainer(depBag, Cdr.ContentRoot, "Extra Content Root");
        Resource child2 = addChildContainer(depBag, Cdr.AdminUnit, "Stray Unit");
        Resource child3 = addChildContainer(depBag, Cdr.Collection, "Stray Collection");
        Resource child4 = addChildContainer(depBag, Cdr.Folder, "Stray Folder");
        Resource child5 = addChildContainer(depBag, Cdr.Work, "Stray Work");

        job.closeModel();

        try {
            job.run();
            fail();
        } catch (JobFailedException e) {
            assertChildrenFailed(e, child1, child2, child3, child4, child5);
        }
    }

    @Test
    public void work_AddFile_InsufficientPermissions() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
            rootObj.addMember(unitObj);
            CollectionObject collObj = repoObjFactory.createCollectionObject(null);
            unitObj.addMember(collObj);
            FolderObject folderObj = repoObjFactory.createFolderObject(null);
            collObj.addMember(folderObj);
            WorkObject workObj = repoObjFactory.createWorkObject(null);
            folderObj.addMember(workObj);

            treeIndexer.indexAll(baseAddress);

            setDestinationAndPermissions(workObj.getPid(), DEPOSITOR_NAME, "no_one_important");

            addChildFile(depBag, "File1", true);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void file_AddChild() throws Exception {
        AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
        rootObj.addMember(unitObj);
        CollectionObject collObj = repoObjFactory.createCollectionObject(null);
        unitObj.addMember(collObj);
        FolderObject folderObj = repoObjFactory.createFolderObject(null);
        collObj.addMember(folderObj);
        WorkObject workObj = repoObjFactory.createWorkObject(null);
        folderObj.addMember(workObj);

        var filePid = pidMinter.mintContentPid();
        var storageUri = storageLocationTestHelper.makeTestStorageUri(DatastreamPids.getOriginalFilePid(filePid));
        FileUtils.writeStringToFile(new File(storageUri), "content", UTF_8);
        FileObject fileObj = workObj.addDataFile(filePid, storageUri, "file.txt", "text/plain", null, null, null);

        treeIndexer.indexAll(baseAddress);

        setDestinationAndPermissions(fileObj.getPid(), DEPOSITOR_NAME, GLOBAL_INGESTOR_PRINC);

        addChildFile(depBag, "File1", true);

        job.closeModel();

        try {
            job.run();
            fail();
        } catch (JobFailedException e) {
            String details = e.getDetails();
            assertTrue(details.contains("types does not support children"));
        }
    }

    @Test
    public void unit_AddNestedChildren() throws Exception {
        AdminUnit unitObj = repoObjFactory.createAdminUnit(null);
        rootObj.addMember(unitObj);

        treeIndexer.indexAll(baseAddress);

        setDestinationAndPermissions(unitObj.getPid(), DEPOSITOR_NAME, ADMIN_PRINC);

        Bag collBag = addChildContainer(depBag, Cdr.Collection, "New Collection");
        Bag folderBag = addChildContainer(collBag, Cdr.Folder, "Nested Folder");
        addChildContainer(folderBag, Cdr.Work, "Nested work");

        job.closeModel();

        job.run();
    }

    private void setDestinationAndPermissions(PID destPid, String depositor, String groups) {
        Map<String, String> status = new HashMap<>();
        status.put(DepositField.containerId.name(), destPid.getRepositoryPath());
        status.put(DepositField.permissionGroups.name(), groups);
        status.put(DepositField.depositorName.name(), depositor);
        depositStatusFactory.save(depositUUID, status);
    }

    private Bag addChildContainer(Bag parent, Resource childType, String label) {
        PID childPid = pidMinter.mintContentPid();
        Bag childBag = depModel.createBag(childPid.getRepositoryPath());
        childBag.addProperty(RDF.type, childType);
        childBag.addProperty(CdrDeposit.label, label);
        parent.add(childBag);

        return childBag;
    }

    private Resource addChildFile(Bag parent, String label, boolean withType) {
        PID childPid = pidMinter.mintContentPid();
        Resource childResc = depModel.createResource(childPid.getRepositoryPath());
        if (withType) {
            childResc.addProperty(RDF.type, Cdr.FileObject);
        }
        childResc.addProperty(CdrDeposit.label, label);
        parent.add(childResc);

        return childResc;
    }
}
