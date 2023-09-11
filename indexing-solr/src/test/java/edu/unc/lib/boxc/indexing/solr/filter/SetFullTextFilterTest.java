package edu.unc.lib.boxc.indexing.solr.filter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;

/**
 * @author harring
 */
public class SetFullTextFilterTest {
    private AutoCloseable closeable;

    @TempDir
    public Path tempDir;

    private File derivativeDir;

    @Mock
    private DocumentIndexingPackageDataLoader loader;
    private DocumentIndexingPackage dip;
    private PID filePid;
    private PID workPid;
    @Mock
    private WorkObject workObj;
    @Mock
    private FileObject fileObj;

    private DerivativeService derivativeService;

    private final static String EXAMPLE_TEXT = "some text";

    private DocumentIndexingPackageFactory factory;

    private SetFullTextFilter filter;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        derivativeDir = tempDir.resolve("testFolder").toFile();
        Files.createDirectory(tempDir.resolve("testFolder"));

        factory = new DocumentIndexingPackageFactory();
        factory.setDataLoader(loader);

        derivativeService = new DerivativeService();
        derivativeService.setDerivativeDir(derivativeDir.getAbsolutePath());

        filePid = PIDs.get(UUID.randomUUID().toString());
        workPid = PIDs.get(UUID.randomUUID().toString());

        when(fileObj.getPid()).thenReturn(filePid);
        when(workObj.getPid()).thenReturn(workPid);

        filter = new SetFullTextFilter();
        filter.setDerivativeService(derivativeService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testFullTextWithWorkObject() throws Exception {
        dip = factory.createDip(workPid);

        createFullTextDerivative(filePid, EXAMPLE_TEXT);

        when(loader.getContentObject(dip)).thenReturn(workObj);
        when(workObj.getPrimaryObject()).thenReturn(fileObj);

        filter.filter(dip);

        assertNull(dip.getDocument().getFullText());
    }

    @Test
    public void testFullTextWithFileObject() throws Exception {
        dip = factory.createDip(filePid);
        createFullTextDerivative(filePid, EXAMPLE_TEXT);

        when(loader.getContentObject(dip)).thenReturn(fileObj);

        filter.filter(dip);

        assertEquals(EXAMPLE_TEXT, dip.getDocument().getFullText());
    }

    @Test
    public void testNoFullText() throws Exception {
        dip = factory.createDip(filePid);

        when(loader.getContentObject(dip)).thenReturn(fileObj);

        filter.filter(dip);

        assertNull(dip.getDocument().getFullText());
    }

    @Test
    public void testNotWorkOrFile() throws Exception {
        FolderObject folder = mock(FolderObject.class);
        when(folder.getPid()).thenReturn(workPid);

        dip = factory.createDip(workPid);
        createFullTextDerivative(workPid, EXAMPLE_TEXT);

        when(loader.getContentObject(dip)).thenReturn(folder);

        filter.filter(dip);

        assertNull(dip.getDocument().getFullText());
    }

    private void createFullTextDerivative(PID pid, String text) throws Exception {
        Path path = derivativeService.getDerivativePath(pid, DatastreamType.FULLTEXT_EXTRACTION);
        FileUtils.writeStringToFile(path.toFile(), text, UTF_8);
    }
}
