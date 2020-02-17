package edu.unc.lib.deposit.fcrepo4;

import edu.unc.lib.dl.fcrepo4.*;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.nio.file.Path;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.DepositConstants.TECHMD_DIR;
import static org.mockito.Mockito.*;

public class RegisterToLongleafJobTest extends AbstractDepositJobTest {

    private final static String LOC1_ID = "loc1";
    private final static String LOC2_ID = "loc2";
    private final static String SOURCE_ID = "source1";
    private final static String DEPOSITS_SOURCE_ID = "deposits";
    private final static String FILE_CONTENT1 = "Some content";
    private final static String FILE_CONTENT2 = "Other stuff";

    private RegisterToLongleafJob job;
    private PID depositPid;
    private Bag depBag;
    private Model model;
    private File techmdDir;
    private Path storageLocPath;

    @Mock
    private FileObject mockFileObj;
    @Mock
    private BinaryObject mockBinaryObj;

    @Before
    public void init() throws Exception {
        Dataset dataset = TDBFactory.createDataset();

        job = new RegisterToLongleafJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "dataset", dataset);

        job.init();

        depositPid = job.getDepositPID();

        FileUtils.copyDirectory(new File("src/test/resources/examples"), depositDir);

        techmdDir = new File(depositDir, TECHMD_DIR);
        techmdDir.mkdir();

        storageLocPath = tmpFolder.newFolder("storageLoc").toPath();

        when(mockFileObj.getOriginalFile()).thenReturn(mockBinaryObj);

        // Get a writeable model
        model = job.getWritableModel();
        depBag = model.createBag(depositPid.getRepositoryPath());

        depBag.addProperty(Cdr.storageLocation, LOC1_ID);
    }

    @Test
    public void registerFileToLongleaf() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        workBag.addProperty(Cdr.primaryObject, fileResc);
        workBag.addProperty(CdrDeposit.storageUri, LOC2_ID);
        job.closeModel();

        job.run();
    }

    // test for deposit with no files
    // test for deposit with single file
    // test for deposit with multiple files
    // test for deposit with binary and mods files
    // test for deposit with files that have not been ingested


    private Resource addFileObject(Bag parent, String content, boolean withFits) throws Exception {
        PID objPid = makePid();
        Resource objResc = model.getResource(objPid.getRepositoryPath());
        objResc.addProperty(RDF.type, Cdr.FileObject);

        File originalFile = storageLocPath.resolve(objPid.getId() + ".txt").toFile();
        FileUtils.writeStringToFile(originalFile, content, "UTF-8");
        objResc.addProperty(CdrDeposit.stagingLocation, originalFile.toPath().toUri().toString());
        objResc.addProperty(CdrDeposit.storageUri, LOC2_ID);

        if (withFits) {
            File fitsFile = new File(techmdDir, objPid.getId() + ".xml");
            fitsFile.createNewFile();
        }

        parent.add(objResc);
        return objResc;
    }

    private Bag addContainerObject(Bag parent, Resource type) {
        PID objPid = makePid();
        Bag objBag = model.createBag(objPid.getRepositoryPath());
        objBag.addProperty(RDF.type, type);
        parent.add(objBag);

        return objBag;
    }
}
