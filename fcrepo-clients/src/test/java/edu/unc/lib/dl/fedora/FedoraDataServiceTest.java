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
package edu.unc.lib.dl.fedora;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.util.TripleStoreQueryService.PathInfo;

@Ignore
public class FedoraDataServiceTest extends Assert {
    private FedoraDataService dataService;
    private AccessClient accessClient;
    private ManagementClient managementClient;
    private TripleStoreQueryService tripleStoreQueryService;

    @Before
    public void setup() {
        dataService = new FedoraDataService();
        accessClient = mock(AccessClient.class);
        managementClient = mock(ManagementClient.class);
        tripleStoreQueryService = mock(TripleStoreQueryService.class);

        dataService.setAccessClient(accessClient);
        dataService.setManagementClient(managementClient);
        dataService.setMaxThreads(5);
        dataService.init();
    }

    @Test
    public void getFoxmlSuccessful() throws FedoraException {
        String pid = "uuid:test";

        Document objectXML = new Document();
        final Element root = new Element("digitalObject");
        objectXML.addContent(root);
        when(managementClient.getObjectXML(any(PID.class))).thenReturn(objectXML);

        Document foxml = dataService.getFoxmlViewXML(pid);

        verify(managementClient).getObjectXML(any(PID.class));
        assertTrue(foxml != null);
        assertEquals(foxml.getRootElement().getContentSize(), 1);
    }

    @Test
    public void getFoxmlEmptyBody() throws FedoraException {
        String pid = "uuid:test";

        Document objectXML = new Document();
        when(managementClient.getObjectXML(any(PID.class))).thenReturn(objectXML);

        try {
            dataService.getFoxmlViewXML(pid);
            fail();
        } catch (ServiceException e) {
            //success
        }

    }

    @Test(expected=FedoraException.class)
    public void getFoxmlRetrievalException() throws FedoraException {
        String pid = "uuid:test";

        Document objectXML = new Document();
        final Element root = new Element("digitalObject");
        objectXML.addContent(root);
        when(managementClient.getObjectXML(any(PID.class))).thenThrow(new FedoraException("Fail"));

        dataService.getFoxmlViewXML(pid);
    }

    private void setupObjectView() throws FedoraException, ServiceException {
        //Setup GetFoxml
        Document objectXML = new Document();
        final Element root = new Element("digitalObject");
        objectXML.addContent(root);
        when(managementClient.getObjectXML(any(PID.class))).thenReturn(objectXML);

        PID parent = new PID("uuid:collection");

        //Setup getPathInfo
        List<PathInfo> pathInfo = new ArrayList<>();
        PathInfo pathNode = new PathInfo();
        pathNode.setPid(parent);
        pathNode.setLabel("Collection");
        pathNode.setSlug("Collection");
        pathInfo.add(pathNode);

        when(tripleStoreQueryService.lookupRepositoryPathInfo(any(PID.class))).thenReturn(pathInfo);

        //Setup getParentCollection
        when(tripleStoreQueryService.fetchParentCollection(any(PID.class))).thenReturn(parent);

        //Setup GetOrderWithinParent
        when(tripleStoreQueryService.fetchContainer(any(PID.class))).thenReturn(parent);
        String mdcontents = "<m:structMap xmlns:m=\"http://www.loc.gov/METS/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            + "<m:div TYPE=\"Container\">"
            + "<m:div ID=\"uuid:test\" ORDER=\"2\"/>"
            + "</m:div>"
            + "</m:structMap>";
        MIMETypedStream mdcontentsDS = mock(MIMETypedStream.class);
        when(mdcontentsDS.getStream()).thenReturn(mdcontents.getBytes());
        when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString()))
            .thenReturn(mdcontentsDS);
    }

    @Test
    public void getObjectViewComplete() throws FedoraException {
        String pid = "uuid:test";

        this.setupObjectView();

        Document objectView = dataService.getObjectViewXML(pid);
        assertNotNull(objectView);
        verify(tripleStoreQueryService).lookupRepositoryPathInfo(any(PID.class));
        verify(managementClient).getObjectXML(any(PID.class));
        verify(tripleStoreQueryService).fetchFirstBySubjectAndPredicate(any(PID.class), anyString());
        verify(tripleStoreQueryService).fetchParentCollection(any(PID.class));
        assertEquals(objectView.getRootElement().getContentSize(), 4);
    }


    @Test
    public void getObjectViewNoPath() throws FedoraException {
        String pid = "uuid:test";

        this.setupObjectView();

        //Setup getPathInfo, with no path nodes
        List<PathInfo> pathInfo = new ArrayList<>();
        when(tripleStoreQueryService.lookupRepositoryPathInfo(any(PID.class))).thenReturn(pathInfo);

        Document objectView = dataService.getObjectViewXML(pid);
        assertNotNull(objectView);
        verify(tripleStoreQueryService).lookupRepositoryPathInfo(any(PID.class));
        verify(managementClient).getObjectXML(any(PID.class));
        verify(tripleStoreQueryService).lookupRepositoryPathInfo(any(PID.class));
        verify(tripleStoreQueryService).fetchParentCollection(any(PID.class));
        assertEquals(objectView.getRootElement().getContentSize(), 3);
    }

    @Test
    public void getObjectViewNoPathFatal() throws FedoraException {
        String pid = "uuid:test";

        this.setupObjectView();

        //Setup getPathInfo, with no path nodes
        List<PathInfo> pathInfo = new ArrayList<>();
        when(tripleStoreQueryService.lookupRepositoryPathInfo(any(PID.class))).thenReturn(pathInfo);

        Document objectView = null;
        try {
            objectView = dataService.getObjectViewXML(pid, true);
            fail();
        } catch (ServiceException e) {
            assertNull(objectView);
            verify(tripleStoreQueryService).lookupRepositoryPathInfo(any(PID.class));
        }
    }

    @Test
    public void getObjectViewMissingFields() throws FedoraException {
        String pid = "uuid:test";

        this.setupObjectView();

        when(tripleStoreQueryService.fetchParentCollection(any(PID.class))).thenReturn(null);

        //no md contents match
        String mdcontents = "<m:structMap xmlns:m=\"http://www.loc.gov/METS/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            + "<m:div TYPE=\"Container\">"
            + "<m:div ID=\"uuid:nothere\" ORDER=\"2\"/>"
            + "</m:div>"
            + "</m:structMap>";
        MIMETypedStream mdcontentsDS = mock(MIMETypedStream.class);
        when(mdcontentsDS.getStream()).thenReturn(mdcontents.getBytes());
        when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString()))
            .thenReturn(mdcontentsDS);

        Document objectView = dataService.getObjectViewXML(pid);

        assertEquals(objectView.getRootElement().getContentSize(), 2);
        verify(tripleStoreQueryService).lookupRepositoryPathInfo(any(PID.class));
        verify(managementClient).getObjectXML(any(PID.class));
        verify(tripleStoreQueryService).lookupRepositoryPathInfo(any(PID.class));
        verify(tripleStoreQueryService).fetchParentCollection(any(PID.class));
    }

    @Test
    public void getObjectViewMissingFieldsFatal() throws FedoraException {
        String pid = "uuid:test";

        this.setupObjectView();

        when(tripleStoreQueryService.fetchParentCollection(any(PID.class))).thenReturn(null);

        //no md contents match
        String mdcontents = "<m:structMap xmlns:m=\"http://www.loc.gov/METS/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            + "<m:div TYPE=\"Container\">"
            + "<m:div ID=\"uuid:nothere\" ORDER=\"2\"/>"
            + "</m:div>"
            + "</m:structMap>";
        MIMETypedStream mdcontentsDS = mock(MIMETypedStream.class);
        when(mdcontentsDS.getStream()).thenReturn(mdcontents.getBytes());
        when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString()))
            .thenReturn(mdcontentsDS);

        Document objectView = null;
        try {
            objectView = dataService.getObjectViewXML(pid, true);
        } catch (ServiceException e) {
            fail();
        }

        assertEquals(objectView.getRootElement().getContentSize(), 2);
        verify(tripleStoreQueryService).lookupRepositoryPathInfo(any(PID.class));
        verify(managementClient).getObjectXML(any(PID.class));
        verify(tripleStoreQueryService).lookupRepositoryPathInfo(any(PID.class));
        verify(tripleStoreQueryService).fetchParentCollection(any(PID.class));
    }

    @Test
    public void getObjectViewNoFoxmlFatal() throws FedoraException {
        String pid = "uuid:test";

        this.setupObjectView();

        when(managementClient.getObjectXML(any(PID.class))).thenReturn(new Document());

        Document objectView = null;
        try {
            objectView = dataService.getObjectViewXML(pid, true);
            fail();
        } catch (ServiceException e) {
            assertNull(objectView);
            verify(managementClient).getObjectXML(any(PID.class));
        }
    }
}
