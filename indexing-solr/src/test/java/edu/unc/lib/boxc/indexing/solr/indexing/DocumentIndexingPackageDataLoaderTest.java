package edu.unc.lib.boxc.indexing.solr.indexing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.jdom2.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectLoaderImpl;

/**
 *
 * @author bbpennel
 *
 */
public class DocumentIndexingPackageDataLoaderTest {

    private DocumentIndexingPackageDataLoader dataLoader;
    private AutoCloseable closeable;

    @Mock
    private RepositoryObjectLoaderImpl repoObjLoader;
    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private PID pid;

    @Mock
    private ContentObject contentObj;
    @Mock
    private BinaryObject modsBinary;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        dataLoader = new DocumentIndexingPackageDataLoader();
        dataLoader.setRepositoryObjectLoader(repoObjLoader);

        when(dip.getPid()).thenReturn(pid);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testLoadMods() throws Exception {
        InputStream modsStream = new FileInputStream(new File(
                "src/test/resources/datastream/inventoryMods.xml"));

        when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(contentObj);
        when(contentObj.getDescription()).thenReturn(modsBinary);
        when(modsBinary.getBinaryStream()).thenReturn(modsStream);

        Element modsElement = dataLoader.loadMods(dip);

        assertNotNull(modsElement);
        assertEquals("mods", modsElement.getName());

        verify(repoObjLoader).getRepositoryObject(any(PID.class));
    }

    @Test
    public void testLoadNoMods() throws Exception {

        when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(contentObj);
        when(contentObj.getDescription()).thenReturn(null);

        Element modsElement = dataLoader.loadMods(dip);

        assertNull(modsElement);

        verify(repoObjLoader).getRepositoryObject(any(PID.class));
    }

    @Test
    public void testLoadBadMods() throws Exception {
        InputStream badModsStream = new ByteArrayInputStream("<mods:mod".getBytes());

        when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(contentObj);
        when(contentObj.getDescription()).thenReturn(modsBinary);
        when(modsBinary.getBinaryStream()).thenReturn(badModsStream);

        Element modsElement = dataLoader.loadMods(dip);

        assertNull(modsElement);
    }
}
