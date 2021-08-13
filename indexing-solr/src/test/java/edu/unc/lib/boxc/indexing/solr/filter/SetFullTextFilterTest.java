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
package edu.unc.lib.boxc.indexing.solr.filter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.filter.SetFullTextFilter;
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

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

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

    @Before
    public void setup() throws Exception {
        initMocks(this);

        derivativeDir = tempDir.newFolder();

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

    @Test
    public void testFullTextWithWorkObject() throws Exception {
        dip = factory.createDip(workPid);

        createFullTextDerivative(filePid, EXAMPLE_TEXT);

        when(loader.getContentObject(dip)).thenReturn(workObj);
        when(workObj.getPrimaryObject()).thenReturn(fileObj);

        filter.filter(dip);

        assertEquals(EXAMPLE_TEXT, dip.getDocument().getFullText());
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
