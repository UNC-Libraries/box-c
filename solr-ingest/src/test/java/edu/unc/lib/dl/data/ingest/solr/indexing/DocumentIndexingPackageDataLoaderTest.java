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
package edu.unc.lib.dl.data.ingest.solr.indexing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author bbpennel
 *
 */
public class DocumentIndexingPackageDataLoaderTest {

    private DocumentIndexingPackageDataLoader dataLoader;

    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private PID pid;

    @Mock
    private ContentObject contentObj;
    @Mock
    private BinaryObject modsBinary;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        dataLoader = new DocumentIndexingPackageDataLoader();
        dataLoader.setRepositoryObjectLoader(repoObjLoader);

        when(pid.getPid()).thenReturn("uuid:" + UUID.randomUUID().toString());
        when(dip.getPid()).thenReturn(pid);
    }

    @Test
    public void testLoadMods() throws Exception {
        InputStream modsStream = new FileInputStream(new File(
                "src/test/resources/datastream/inventoryMods.xml"));

        when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(contentObj);
        when(contentObj.getMODS()).thenReturn(modsBinary);
        when(modsBinary.getBinaryStream()).thenReturn(modsStream);

        Element modsElement = dataLoader.loadMods(dip);

        assertNotNull(modsElement);
        assertEquals("mods", modsElement.getName());

        verify(repoObjLoader).getRepositoryObject(any(PID.class));
    }

    @Test
    public void testLoadNoMods() throws Exception {

        when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(contentObj);
        when(contentObj.getMODS()).thenReturn(null);

        Element modsElement = dataLoader.loadMods(dip);

        assertNull(modsElement);

        verify(repoObjLoader).getRepositoryObject(any(PID.class));
    }

    @Test(expected = IndexingException.class)
    public void testLoadBadMods() throws Exception {
        InputStream badModsStream = new ByteArrayInputStream("<mods:mod".getBytes());

        when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(contentObj);
        when(contentObj.getMODS()).thenReturn(modsBinary);
        when(modsBinary.getBinaryStream()).thenReturn(badModsStream);

        dataLoader.loadMods(dip);
    }
}
