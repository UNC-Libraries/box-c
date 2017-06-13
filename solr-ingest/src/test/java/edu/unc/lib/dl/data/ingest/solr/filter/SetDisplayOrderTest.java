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
package edu.unc.lib.dl.data.ingest.solr.filter;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class SetDisplayOrderTest extends Assert {

    private DocumentIndexingPackageFactory factory;
    private DocumentIndexingPackageDataLoader loader;

    @Mock
    private ManagementClient managementClient;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        loader = new DocumentIndexingPackageDataLoader();
        loader.setManagementClient(managementClient);

        factory = new DocumentIndexingPackageFactory();
        factory.setDataLoader(loader);
    }

    @Test
    public void fromParents() throws Exception {
        DocumentIndexingPackage dip = factory.createDip("uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194");

        DocumentIndexingPackage parentDIP = setupDip("uuid:parent",
                "src/test/resources/foxml/aggregateSplitDepartments.xml");
        dip.setParentDocument(parentDIP);

        SetDisplayOrderFilter filter = new SetDisplayOrderFilter();
        filter.filter(dip);

        assertEquals(2, dip.getDocument().getDisplayOrder().longValue());
        dip.setPid(new PID("info:fedora/uuid:9a7f19d7-5f1d-44f9-9c3d-3ff4f7dac42d"));
        filter.filter(dip);
        assertEquals(1, dip.getDocument().getDisplayOrder().longValue());
    }

    @Test
    public void fromRetrievedParent() throws Exception {
        DocumentIndexingPackage dip = factory.createDip("uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194");

        DocumentIndexingPackage parentDIP = setupDip("uuid:parent",
                "src/test/resources/foxml/aggregateSplitDepartments.xml");
        dip.setParentDocument(parentDIP);

        TripleStoreQueryService tsqs = mock(TripleStoreQueryService.class);
        when(tsqs.fetchByPredicateAndLiteral(anyString(), any(PID.class))).thenReturn(Arrays.asList(new PID("info:fedora/uuid:parent")));

        SetDisplayOrderFilter filter = new SetDisplayOrderFilter();
        filter.filter(dip);

        assertEquals(2, dip.getDocument().getDisplayOrder().longValue());
        dip.setPid(new PID("info:fedora/uuid:9a7f19d7-5f1d-44f9-9c3d-3ff4f7dac42d"));
    }

    @Test
    public void fromAncestorParent() throws Exception {
        DocumentIndexingPackage dip = factory.createDip("uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194");

        DocumentIndexingPackage parentDIP = setupDip("uuid:parent",
                "src/test/resources/foxml/aggregateSplitDepartments.xml");
        dip.setParentDocument(parentDIP);

        SetDisplayOrderFilter filter = new SetDisplayOrderFilter();
        filter.filter(dip);

        assertEquals(2, dip.getDocument().getDisplayOrder().longValue());
        dip.setPid(new PID("info:fedora/uuid:9a7f19d7-5f1d-44f9-9c3d-3ff4f7dac42d"));
    }

    @Test
    public void fromParentsNoMDContents() throws Exception {
        DocumentIndexingPackage dip = factory.createDip("uuid:a4fa0296-1ce7-42a1-b74d-0222afd98194");

        DocumentIndexingPackage parentDIP = setupDip("uuid:parent",
                "src/test/resources/foxml/folderNoMDContents.xml");
        dip.setParentDocument(parentDIP);

        SetDisplayOrderFilter filter = new SetDisplayOrderFilter();
        filter.filter(dip);

        assertNull(dip.getDocument().getDisplayOrder());
    }

    private DocumentIndexingPackage setupDip(String pid, String foxmlFilePath) throws Exception {
        DocumentIndexingPackage dip = factory.createDip(pid);

        SAXBuilder builder = new SAXBuilder();
        Document foxml = builder.build(new FileInputStream(new File(foxmlFilePath)));
        dip.setFoxml(foxml);

        return dip;
    }
}
