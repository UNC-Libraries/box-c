package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
public class SetAltTextFilterTest {
    private SetAltTextFilter filter;

    @Mock
    private ContentObject contentObject;

    @Mock
    private FileObject fileObject;
    @Mock
    private BinaryObject binaryObject;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;

    private PID pid;
    private IndexDocumentBean document;
    private DocumentIndexingPackage dip;
    private DocumentIndexingPackageFactory factory;

    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        pid = TestHelper.makePid();
        factory = new DocumentIndexingPackageFactory();
        filter = new SetAltTextFilter();
        filter.setRepositoryObjectLoader(repositoryObjectLoader);
        dip = factory.createDip(pid);
        document = dip.getDocument();
        when(fileObject.getPid()).thenReturn(pid);
        when(repositoryObjectLoader.getBinaryObject(any(PID.class))).thenReturn(binaryObject);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testFilterSetsAltText() throws Exception {
        String altTextContent = "Sample Alt Text";
        var altTextStream = new ByteArrayInputStream(altTextContent.getBytes());

        dip.setContentObject(fileObject);
        when(binaryObject.getBinaryStream()).thenReturn(altTextStream);

        filter.filter(dip);

        assertEquals(altTextContent, document.getAltText());
    }

    @Test
    public void testFilterHandlesMissingAltText() throws Exception {
        dip.setContentObject(fileObject);
        when(repositoryObjectLoader.getBinaryObject(any(PID.class))).thenThrow(new NotFoundException("Not found"));

        filter.filter(dip);

        assertNull(document.getAltText());
    }

    @Test
    public void testFilterHandlesIOException() throws Exception {
        dip.setContentObject(fileObject);
        String altTextContent = "Bad alt text";
        var altTextStream = new ByteArrayInputStream(altTextContent.getBytes());
        when(binaryObject.getBinaryStream()).thenReturn(altTextStream);

        try (MockedStatic<IOUtils> mockedStatic = mockStatic(IOUtils.class)) {
            mockedStatic.when(() -> IOUtils.toString(any(InputStream.class), eq(UTF_8)))
                    .thenThrow(new IOException("Test IO Exception"));

            assertThrows(IndexingException.class, () -> filter.filter(dip));
        }
    }

    @Test
    public void testFilterNonFileObject() throws Exception {
        dip.setContentObject(contentObject);

        filter.filter(dip);

        assertNull(document.getAltText());
    }
}
