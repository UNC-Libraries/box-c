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
package edu.unc.lib.dl.services;

import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.MD_CONTENTS;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.RELS_EXT;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static edu.unc.lib.dl.util.ContentModelHelper.Relationship.contains;
import static edu.unc.lib.dl.util.ContentModelHelper.Relationship.removedChild;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.fedora.DatastreamDocument;
import edu.unc.lib.dl.fedora.FedoraAccessControlService;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.OptimisticLockException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.Datastream;
import edu.unc.lib.dl.fedora.types.MIMETypedStream.Header;
import edu.unc.lib.dl.fedora.types.Property;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.JDOMQueryUtil;

/**
 * @author bbpennel
 * @date Jan 30, 2015
 */
@Ignore
public class DigitalObjectManagerMoveTest {

    private DigitalObjectManagerImpl digitalMan;

    @Mock
    private ManagementClient managementClient;
    @Mock
    private AccessClient accessClient;
    @Mock
    private TripleStoreQueryService tripleStoreQueryService;
    @Mock
    private FedoraAccessControlService aclService;

    private Header dsHeaders;

    private PID destPID;

    private final PID source1PID = new PID("uuid:source1");
    private final PID source2PID = new PID("uuid:source2");

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        dsHeaders = mock(Header.class);
        when(dsHeaders.getProperty()).thenReturn(Arrays.asList(new Property[0]));

        digitalMan = new DigitalObjectManagerImpl();
        digitalMan.setAvailable(true, "available");
        digitalMan.setAccessClient(accessClient);
        digitalMan.setForwardedManagementClient(null);
        digitalMan.setManagementClient(managementClient);
        digitalMan.setAclService(aclService);
        digitalMan.setTripleStoreQueryService(tripleStoreQueryService);


        destPID = new PID("uuid:destination");

        when(tripleStoreQueryService.lookupRepositoryAncestorPids(destPID)).thenReturn(
                Arrays.asList(new PID("uuid:Collections"), new PID("uuid:collection")));

        when(tripleStoreQueryService.lookupContentModels(destPID)).thenReturn(Arrays.asList(CONTAINER.getURI()));

        makeMatcherPair("/fedora/containerRELSEXT2.xml", destPID);

        when(aclService.hasAccess(any(PID.class), any(AccessGroupSet.class), eq(Permission.addRemoveContents)))
                .thenReturn(true);
    }

    @Test(expected = IngestException.class)
    public void moveParentIntoChildTest() throws Exception {
        List<PID> moving = Arrays.asList(new PID("uuid:child1"), new PID("uuid:collection"));
        digitalMan.move(moving, destPID, "user", "");
    }

    @Test(expected = IngestException.class)
    public void destinationDoesNotExistTest() throws Exception {
        when(tripleStoreQueryService.lookupRepositoryAncestorPids(destPID)).thenReturn(null);

        List<PID> moving = Arrays.asList(new PID("uuid:child1"), new PID("uuid:child2"));
        digitalMan.move(moving, destPID, "user", "");
    }

    public static class PairedMatcher extends ArgumentMatcher<Document> {
        public Document lastMatchedDoc;

        @Override
        public boolean matches(Object argument) {
            lastMatchedDoc = (Document) argument;
            return true;
        }
    }

    private class PairedAnswer implements Answer<Document> {

        public PairedAnswer(Document startingValue, PairedMatcher matcher) {
            this.startingValue = startingValue;
            this.matcher = matcher;
        }

        private final Document startingValue;
        private final PairedMatcher matcher;

        @Override
        public Document answer(InvocationOnMock invocation) throws Throwable {
            if (matcher.lastMatchedDoc == null) {
                return startingValue;
            }
            return matcher.lastMatchedDoc.clone();
        }
    }

    private DatastreamDocument makeMatcherPair(String relsPath, PID targetPID) throws Exception {
        InputStream relsExtStream = this.getClass().getResourceAsStream(relsPath);
        DatastreamDocument dsDoc = mock(DatastreamDocument.class);
        Document doc = ClientUtils.parseXML(IOUtils.toByteArray(relsExtStream));

        // PairedMatcher allows future calls to get the document to contain the last modified version
        PairedMatcher sourceRelsExtMatcher = new PairedMatcher();
        PairedAnswer sourceRelsExtAnswer = new PairedAnswer(doc, sourceRelsExtMatcher);
        when(dsDoc.getDocument()).thenAnswer(sourceRelsExtAnswer);

        when(managementClient.getRELSEXTWithRetries(eq(targetPID)))
            .thenReturn(dsDoc);

        doNothing().when(managementClient)
                .modifyDatastream(eq(targetPID), eq(RELS_EXT.getName()), anyString(),
                anyString(), argThat(sourceRelsExtMatcher));

        return dsDoc;
    }

    @Test
    public void oneSourceTest() throws Exception {

        makeMatcherPair("/fedora/containerRELSEXT1.xml", source1PID);

        List<PID> moving = Arrays.asList(new PID("uuid:child1"), new PID("uuid:child5"));

        when(tripleStoreQueryService.queryResourceIndex(anyString()))
                .thenReturn(Arrays.asList(Arrays.asList(source1PID.getPid())));

        digitalMan.move(moving, destPID, "user", "");

        verify(managementClient, times(2)).getRELSEXTWithRetries(eq(source1PID));

        verifySourceMoved(source1PID, moving, 10);
        verifyDestinationMoved(moving, 9);
    }

    private void makeDatastream(String contentPath, String datastream, PID pid) throws Exception {
        InputStream mdContentsStream = this.getClass().getResourceAsStream(contentPath);
        DatastreamDocument dsDoc = mock(DatastreamDocument.class);
        Document doc = ClientUtils.parseXML(IOUtils.toByteArray(mdContentsStream));
        when(dsDoc.getDocument()).thenReturn(doc);

        when(managementClient.getXMLDatastreamIfExists(eq(pid), eq(datastream)))
            .thenReturn(dsDoc);
    }

    @Test
    public void oneSourceWithMDContents() throws Exception {
        makeDatastream("/fedora/mdContents1.xml", MD_CONTENTS.getName(), source1PID);

        oneSourceTest();

        verify(managementClient).modifyDatastream(eq(source1PID), eq(MD_CONTENTS.getName()), anyString(),
                anyString(), any(Document.class));
    }

    @Test
    public void oneSourceLockFailTest() throws Exception {
        makeDatastream("/fedora/mdContents1.xml", MD_CONTENTS.getName(), source1PID);

        PairedMatcher sourceRelsExtMatcher = new PairedMatcher();
        InputStream relsExtStream = this.getClass().getResourceAsStream("/fedora/containerRELSEXT1.xml");
        Document doc = ClientUtils.parseXML(IOUtils.toByteArray(relsExtStream));
        PairedAnswer sourceRelsExtAnswer = new PairedAnswer(doc, sourceRelsExtMatcher);

        // Throw locking exception on first attempt to rewrite source RELS-EXT
        doThrow(new OptimisticLockException(""))
                .doNothing()
                .when(managementClient)
                .modifyDatastream(eq(source1PID), eq(RELS_EXT.getName()), anyString(), anyString(),
                        argThat(sourceRelsExtMatcher));

        doThrow(new OptimisticLockException(""))
                .doNothing()
                .when(managementClient)
                .modifyDatastream(eq(destPID), eq(RELS_EXT.getName()), anyString(), anyString(),
                        any(Document.class));

        doThrow(new OptimisticLockException(""))
                .doNothing()
                .when(managementClient)
                .modifyDatastream(eq(source1PID), eq(MD_CONTENTS.getName()), anyString(), anyString(),
                        any(Document.class));

        DatastreamDocument dsDoc = mock(DatastreamDocument.class);
        when(dsDoc.getDocument()).thenAnswer(sourceRelsExtAnswer);
        when(managementClient.getRELSEXTWithRetries(eq(source1PID)))
                .thenReturn(dsDoc);

        Datastream ds = mock(Datastream.class);
        when(ds.getCreateDate()).thenReturn(new String());
        when(managementClient.getDatastream(eq(source1PID), eq(RELS_EXT.getName()))).thenReturn(ds);

        List<PID> moving = Arrays.asList(new PID("uuid:child1"), new PID("uuid:child5"));

        when(tripleStoreQueryService.queryResourceIndex(anyString()))
                .thenReturn(Arrays.asList(Arrays.asList(source1PID.getPid())));

        digitalMan.move(moving, destPID, "user", "");

        verify(managementClient, times(3)).getRELSEXTWithRetries(eq(source1PID));

        ArgumentCaptor<Document> sourceRelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);

        verify(managementClient, times(3)).modifyDatastream(eq(source1PID), eq(RELS_EXT.getName()), anyString(),
                anyString(), sourceRelsExtUpdateCaptor.capture());

        List<Document> sourceRelsAnswers = sourceRelsExtUpdateCaptor.getAllValues();

        // Verify that the initial source RELS-EXT update is repeated after failure
        Document sourceRelsExt = sourceRelsAnswers.get(0);
        Set<PID> removed = JDOMQueryUtil.getRelationSet(sourceRelsExt.getRootElement(), removedChild);
        assertEquals("Child tombstones should still be present", 2, removed.size());
        sourceRelsExt = sourceRelsAnswers.get(1);
        removed = JDOMQueryUtil.getRelationSet(sourceRelsExt.getRootElement(), removedChild);
        assertEquals("Child tombstones should still be present on second try", 2, removed.size());

        // Check that tombstones were cleaned up by the end of the operation
        Document cleanRelsExt = sourceRelsAnswers.get(2);
        Set<PID> children = JDOMQueryUtil.getRelationSet(cleanRelsExt.getRootElement(), contains);
        assertEquals("Incorrect number of children in source container after cleanup", 10, children.size());

        removed = JDOMQueryUtil.getRelationSet(cleanRelsExt.getRootElement(), removedChild);
        assertEquals("Child tombstones not cleaned up", 0, removed.size());

        // Verify that the destination had the moved children added to it
        ArgumentCaptor<Document> destRelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);
        verify(managementClient, times(2)).modifyDatastream(eq(destPID), eq(RELS_EXT.getName()), anyString(),
                anyString(), destRelsExtUpdateCaptor.capture());

        Document destRelsExt = destRelsExtUpdateCaptor.getValue();
        children = JDOMQueryUtil.getRelationSet(destRelsExt.getRootElement(), contains);
        assertEquals("Incorrect number of children in destination container after moved", 9, children.size());
        assertTrue("Moved children were not present in destination", children.containsAll(moving));

        verify(managementClient, times(2)).modifyDatastream(eq(source1PID), eq(MD_CONTENTS.getName()),
                anyString(), anyString(), any(Document.class));
    }

    @Test
    public void multiSourceTest() throws Exception {
        List<PID> moving = Arrays.asList(new PID("uuid:child1"), new PID("uuid:child32"));

        makeMatcherPair("/fedora/containerRELSEXT1.xml", source1PID);
        makeMatcherPair("/fedora/containerRELSEXT3.xml", source2PID);

        when(tripleStoreQueryService.queryResourceIndex(anyString()))
                .thenReturn(Arrays.asList(Arrays.asList(source1PID.getPid())))
                .thenReturn(Arrays.asList(Arrays.asList(source2PID.getPid())));

        digitalMan.move(moving, destPID, "user", "");

        verify(managementClient, times(2)).getRELSEXTWithRetries(eq(source1PID));
        verify(managementClient, times(2)).getRELSEXTWithRetries(eq(source2PID));

        verifySourceMoved(source1PID, Arrays.asList(new PID("uuid:child1")), 11);
        verifySourceMoved(source2PID, Arrays.asList(new PID("uuid:child32")), 1);
        verifyDestinationMoved(moving, 9);
    }

    @Test
    public void rollbackNoProblemsTest() throws Exception {
        // Create a document for the source which has no removedChildren
        DatastreamDocument dsDoc = mock(DatastreamDocument.class);
        Document doc = mock(Document.class);
        when(dsDoc.getDocument()).thenReturn(doc);
        when(doc.getRootElement()).thenReturn(
                new Element("RDF", JDOMNamespaceUtil.RDF_NS)
                .addContent(new Element("Description", JDOMNamespaceUtil.RDF_NS)));
        when(managementClient.getRELSEXTWithRetries(any(PID.class)))
                .thenReturn(dsDoc);

        List<PID> moving = Arrays.asList(new PID("uuid:child1"), new PID("uuid:child5"));
        digitalMan.rollbackMove(source1PID, moving);

        // Verify that it doesn't try to change anything when there are no leftover tombstones
        verify(managementClient, never()).modifyDatastream(eq(destPID), eq(RELS_EXT.getName()), anyString(),
                anyString(), any(Document.class));
    }

    @Test
    public void rollbackTest() throws Exception {
        makeMatcherPair("/fedora/containerRELSEXT1.xml", source1PID);

        List<PID> moving = Arrays.asList(new PID("uuid:child1"), new PID("uuid:child5"));

        when(tripleStoreQueryService.fetchContainer(any(PID.class))).thenReturn(source1PID).thenReturn(source1PID)
                .thenReturn(null).thenReturn(null);

        when(tripleStoreQueryService.queryResourceIndex(anyString()))
                .thenReturn(Arrays.asList(Arrays.asList(source1PID.getPid())))
                .thenReturn(Arrays.asList(Arrays.asList(source1PID.getPid())))
                .thenReturn(null).thenReturn(null);

        when(managementClient.getRELSEXTWithRetries(eq(destPID))).thenReturn(null);

        try {
            digitalMan.move(moving, destPID, "user", "");
            fail();
        } catch (IngestException e) {
            // Expected
        }

        ArgumentCaptor<Document> sourceRelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);

        // There should have been three updates to the source RELS-EXT, the initial, rollback the moved, and cleanup
        verify(managementClient, times(3)).modifyDatastream(eq(source1PID), eq(RELS_EXT.getName()), anyString(),
                anyString(), sourceRelsExtUpdateCaptor.capture());

        List<Document> sourceRelsAnswers = sourceRelsExtUpdateCaptor.getAllValues();
        // Check the state of the source after removal but before cleanup
        Document sourceRelsExt = sourceRelsAnswers.get(0);
        Set<PID> children = JDOMQueryUtil.getRelationSet(sourceRelsExt.getRootElement(), contains);
        assertEquals("Incorrect number of children in source container after move", 10, children.size());

        Set<PID> removed = JDOMQueryUtil.getRelationSet(sourceRelsExt.getRootElement(), removedChild);
        assertEquals("Moved child gravestones not correctly set in source container", 2, removed.size());

        // Children should all be back as contains statements as part of the rollback
        Document rbRelsExt = sourceRelsAnswers.get(1);
        children = JDOMQueryUtil.getRelationSet(rbRelsExt.getRootElement(), contains);
        assertEquals("Incorrect number of children in source container after first rollback", 12, children.size());

        removed = JDOMQueryUtil.getRelationSet(rbRelsExt.getRootElement(), removedChild);
        assertEquals("Child tombstones should still be present", 2, removed.size());

        // Should be back to the original set of children relations by the end of rolling back
        Document cleanRelsExt = sourceRelsAnswers.get(2);
        children = JDOMQueryUtil.getRelationSet(cleanRelsExt.getRootElement(), contains);
        assertEquals("Incorrect number of children in source container after rollback cleanup", 12, children.size());

        removed = JDOMQueryUtil.getRelationSet(cleanRelsExt.getRootElement(), removedChild);
        assertEquals("Child tombstones not cleaned up", 0, removed.size());
    }

    @Test
    public void rollbackAfterDestinationUpdateTest() throws Exception {
        DatastreamDocument dsDoc = makeMatcherPair("/fedora/containerRELSEXT1.xml", source1PID);
        makeMatcherPair("/fedora/containerRELSEXT2.xml", destPID);

        List<PID> moving = Arrays.asList(new PID("uuid:child1"), new PID("uuid:child5"));

        when(tripleStoreQueryService.queryResourceIndex(anyString()))
                .thenReturn(Arrays.asList(Arrays.asList(source1PID.getPid())))
                .thenReturn(Arrays.asList(Arrays.asList(source1PID.getPid())))
                .thenReturn(Arrays.asList(Arrays.asList(destPID.getPid())))
                .thenReturn(Arrays.asList(Arrays.asList(destPID.getPid())));

        // Second attempt to get the source RELS-EXT will return null to trigger rollback
        when (managementClient.getRELSEXTWithRetries(eq(source1PID)))
            .thenReturn(dsDoc).thenThrow(new FedoraException("")).thenReturn(dsDoc);

        try {
            digitalMan.move(moving, destPID, "user", "");
            fail();
        } catch (IngestException e) {
            // Expected
        }

        ArgumentCaptor<Document> sourceRelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);

        // There should have been three updates to the source RELS-EXT, the initial, rollback the moved, and cleanup
        verify(managementClient, times(3)).modifyDatastream(eq(source1PID), eq(RELS_EXT.getName()), anyString(),
                anyString(), sourceRelsExtUpdateCaptor.capture());

        List<Document> sourceRelsAnswers = sourceRelsExtUpdateCaptor.getAllValues();
        // Check the state of the source after removal but before cleanup
        Document sourceRelsExt = sourceRelsAnswers.get(0);
        Set<PID> children = JDOMQueryUtil.getRelationSet(sourceRelsExt.getRootElement(), contains);
        assertEquals("Incorrect number of children in source container after move", 10, children.size());

        Set<PID> removed = JDOMQueryUtil.getRelationSet(sourceRelsExt.getRootElement(), removedChild);
        assertEquals("Moved child gravestones not correctly set in source container", 2, removed.size());

        // Should be back to the original set of children relations by the end of rolling back
        Document cleanRelsExt = sourceRelsAnswers.get(2);
        children = JDOMQueryUtil.getRelationSet(cleanRelsExt.getRootElement(), contains);
        assertEquals("Incorrect number of children in source container after rollback cleanup", 12, children.size());

        removed = JDOMQueryUtil.getRelationSet(cleanRelsExt.getRootElement(), removedChild);
        assertEquals("Child tombstones not cleaned up", 0, removed.size());

        ArgumentCaptor<Document> destRelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);
        verify(managementClient, times(2)).modifyDatastream(eq(destPID), eq(RELS_EXT.getName()), anyString(),
                anyString(), destRelsExtUpdateCaptor.capture());

        List<Document> destRelsAnswers = destRelsExtUpdateCaptor.getAllValues();
        // Check the state of the source after removal but before cleanup
        Document destRelsExt = destRelsAnswers.get(0);
        children = JDOMQueryUtil.getRelationSet(destRelsExt.getRootElement(), contains);
        assertEquals("Incorrect number of children in destination container after move", 9, children.size());

        // Should be back to the original set of children relations by the end of rolling back
        Document destRBRelsExt = destRelsAnswers.get(1);
        children = JDOMQueryUtil.getRelationSet(destRBRelsExt.getRootElement(), contains);
        assertEquals("Incorrect number of children in destination container after rollback", 7, children.size());
    }

    @Test
    public void multipleContainersTest() throws Exception {

        makeMatcherPair("/fedora/containerRELSEXT1.xml", source1PID);
        makeMatcherPair("/fedora/containerRELSEXT1.xml", source2PID);

        List<PID> moving = Arrays.asList(new PID("uuid:child1"));

        when(tripleStoreQueryService.queryResourceIndex(anyString()))
                .thenReturn(Arrays.asList(Arrays.asList(source1PID.getPid()),
                        Arrays.asList(source2PID.getPid())));

        digitalMan.move(moving, destPID, "user", "");

        verifySourceMoved(source1PID, moving, 11);
        verifySourceMoved(source2PID, moving, 11);
        verifyDestinationMoved(moving, 8);
    }

    private void verifySourceMoved(PID sourcePID, List<PID> moving, int sourceContainsCount)
            throws Exception {
        ArgumentCaptor<Document> sourceRelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);

        verify(managementClient, times(2)).modifyDatastream(eq(sourcePID), eq(RELS_EXT.getName()), anyString(),
                anyString(), sourceRelsExtUpdateCaptor.capture());

        List<Document> sourceRelsAnswers = sourceRelsExtUpdateCaptor.getAllValues();
        // Check the state of the source after removal but before cleanup
        Document sourceRelsExt = sourceRelsAnswers.get(0);
        Set<PID> children = JDOMQueryUtil.getRelationSet(sourceRelsExt.getRootElement(), contains);
        assertEquals("Incorrect number of children in source container after move", sourceContainsCount, children.size());

        Set<PID> removed = JDOMQueryUtil.getRelationSet(sourceRelsExt.getRootElement(), removedChild);
        assertEquals("Moved child gravestones not correctly set in source container", moving.size(), removed.size());

        // Check that tombstones were cleaned up by the end of the operation
        Document cleanRelsExt = sourceRelsAnswers.get(1);
        children = JDOMQueryUtil.getRelationSet(cleanRelsExt.getRootElement(), contains);
        assertEquals("Incorrect number of children in source container after cleanup", sourceContainsCount, children.size());

        removed = JDOMQueryUtil.getRelationSet(cleanRelsExt.getRootElement(), removedChild);
        assertEquals("Child tombstones not cleaned up", 0, removed.size());

        assertFalse("Moved children were still present in source", children.containsAll(moving));
    }

    private void verifyDestinationMoved(List<PID> moving, int destinationCount)
            throws Exception {
        // Verify that the destination had the moved children added to it
        verify(managementClient).getRELSEXTWithRetries(eq(destPID));
        ArgumentCaptor<Document> destRelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);
        verify(managementClient).modifyDatastream(eq(destPID), eq(RELS_EXT.getName()), anyString(),
                anyString(), destRelsExtUpdateCaptor.capture());


        Document destRelsExt = destRelsExtUpdateCaptor.getValue();
        Set<PID> children = JDOMQueryUtil.getRelationSet(destRelsExt.getRootElement(), contains);
        assertEquals("Incorrect number of children in destination container after moved", destinationCount, children.size());
        assertTrue("Moved children were not present in destination", children.containsAll(moving));
    }
}
