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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.OptimisticLockException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.fedora.types.MIMETypedStream.Header;
import edu.unc.lib.dl.fedora.types.Property;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.util.ContentModelHelper.Relationship;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * @author bbpennel
 * @date Jan 30, 2015
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class DigitalObjectManagerMoveTest {

	@Resource
	private final DigitalObjectManagerImpl digitalMan = null;

	@Resource
	private final ManagementClient managementClient = null;

	@Resource
	private final AccessClient accessClient = null;

	@Resource
	private final TripleStoreQueryService tripleStoreQueryService = null;

	@Resource
	private AccessControlService aclService;

	private Header dsHeaders;

	private PID destPID;

	private final PID source1PID = new PID("uuid:source1");
	private final PID source2PID = new PID("uuid:source2");

	@Before
	public void setUp() throws Exception {
		reset(managementClient);
		reset(accessClient);
		reset(tripleStoreQueryService);
		reset(aclService);

		dsHeaders = mock(Header.class);
		when(dsHeaders.getProperty()).thenReturn(Arrays.asList(new Property[0]));

		digitalMan.setAvailable(true, "available");

		destPID = new PID("uuid:destination");

		when(tripleStoreQueryService.lookupRepositoryAncestorPids(destPID)).thenReturn(
				Arrays.asList(new PID("uuid:Collections"), new PID("uuid:collection")));

		when(tripleStoreQueryService.lookupContentModels(destPID)).thenReturn(Arrays.asList(CONTAINER.getURI()));
		when(tripleStoreQueryService.hasDisseminator(any(PID.class), eq(RELS_EXT.getName()))).thenReturn(true);

		InputStream relsExtStream2 = this.getClass().getResourceAsStream("/fedora/containerRELSEXT2.xml");
		MIMETypedStream mts2 = mock(MIMETypedStream.class);
		when(mts2.getHeader()).thenReturn(dsHeaders);
		when(mts2.getStream()).thenReturn(IOUtils.toByteArray(relsExtStream2));
		when(accessClient.getDatastreamDissemination(eq(destPID), eq(RELS_EXT.getName()), anyString())).thenReturn(
				mts2);

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

	private class PairedAnswer implements Answer<MIMETypedStream> {

		public PairedAnswer(MIMETypedStream startingValue, PairedMatcher matcher) {
			this.startingValue = startingValue;
			this.matcher = matcher;
		}

		private final MIMETypedStream startingValue;
		private final PairedMatcher matcher;

		@Override
		public MIMETypedStream answer(InvocationOnMock invocation) throws Throwable {
			if (matcher.lastMatchedDoc == null)
				return startingValue;

			XMLOutputter outputter = new XMLOutputter(org.jdom2.output.Format.getPrettyFormat());
			String docString = outputter.outputString(matcher.lastMatchedDoc);
			MIMETypedStream mts = mock(MIMETypedStream.class);
			when(mts.getHeader()).thenReturn(dsHeaders);
			when(mts.getStream()).thenReturn(docString.getBytes());
			return mts;
		}
	}

	private void makeMatcherPair(String relsPath, PID targetPID) throws Exception {
		PairedMatcher sourceRelsExtMatcher = new PairedMatcher();
		InputStream relsExtStream = this.getClass().getResourceAsStream(relsPath);
		MIMETypedStream mts = mock(MIMETypedStream.class);
		when(mts.getHeader()).thenReturn(dsHeaders);
		when(mts.getStream()).thenReturn(IOUtils.toByteArray(relsExtStream));
		PairedAnswer sourceRelsExtAnswer = new PairedAnswer(mts, sourceRelsExtMatcher);

		doNothing().when(managementClient)
				.modifyDatastream(eq(targetPID), eq(RELS_EXT.getName()), anyString(),
				anyListOf(String.class),
						anyString(), anyString(), argThat(sourceRelsExtMatcher));

		when(accessClient.getDatastreamDissemination(eq(targetPID), eq(RELS_EXT.getName()), anyString())).thenAnswer(
				sourceRelsExtAnswer);
	}

	@Test
	public void oneSourceTest() throws Exception {

		makeMatcherPair("/fedora/containerRELSEXT1.xml", source1PID);

		List<PID> moving = Arrays.asList(new PID("uuid:child1"), new PID("uuid:child5"));

		when(tripleStoreQueryService.fetchContainer(any(PID.class))).thenReturn(source1PID);

		digitalMan.move(moving, destPID, "user", "");

		verify(accessClient, times(2)).getDatastreamDissemination(eq(source1PID), eq(RELS_EXT.getName()), anyString());

		ArgumentCaptor<Document> sourceRelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);

		verify(managementClient, times(2)).modifyDatastream(eq(source1PID), eq(RELS_EXT.getName()), anyString(),
				anyListOf(String.class), anyString(), anyString(), sourceRelsExtUpdateCaptor.capture());

		List<Document> sourceRelsAnswers = sourceRelsExtUpdateCaptor.getAllValues();
		// Check the state of the source after removal but before cleanup
		Document sourceRelsExt = sourceRelsAnswers.get(0);
		Set<PID> children = getRelationSet(sourceRelsExt.getRootElement(), contains);
		assertEquals("Incorrect number of children in source container after move", 10, children.size());

		Set<PID> removed = getRelationSet(sourceRelsExt.getRootElement(), removedChild);
		assertEquals("Moved child gravestones not correctly set in source container", 2, removed.size());

		// Check that tombstones were cleaned up by the end of the operation
		Document cleanRelsExt = sourceRelsAnswers.get(1);
		children = getRelationSet(cleanRelsExt.getRootElement(), contains);
		assertEquals("Incorrect number of children in source container after cleanup", 10, children.size());

		removed = getRelationSet(cleanRelsExt.getRootElement(), removedChild);
		assertEquals("Child tombstones not cleaned up", 0, removed.size());

		// Verify that the destination had the moved children added to it
		verify(accessClient).getDatastreamDissemination(eq(destPID), eq(RELS_EXT.getName()), anyString());
		ArgumentCaptor<Document> destRelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);
		verify(managementClient).modifyDatastream(eq(destPID), eq(RELS_EXT.getName()), anyString(),
				anyListOf(String.class), anyString(), anyString(), destRelsExtUpdateCaptor.capture());
		assertFalse("Moved children were still present in source", children.containsAll(moving));

		Document destRelsExt = destRelsExtUpdateCaptor.getValue();
		children = getRelationSet(destRelsExt.getRootElement(), contains);
		assertEquals("Incorrect number of children in destination container after moved", 9, children.size());
		assertTrue("Moved children were not present in destination", children.containsAll(moving));
	}

	private void makeDatastream(String contentPath, String datastream, PID pid) throws Exception {
		when(tripleStoreQueryService.hasDisseminator(eq(pid), eq(datastream))).thenReturn(true);
		InputStream mdContentsStream = this.getClass().getResourceAsStream(contentPath);
		MIMETypedStream mts = mock(MIMETypedStream.class);
		when(mts.getHeader()).thenReturn(dsHeaders);
		when(mts.getStream()).thenReturn(IOUtils.toByteArray(mdContentsStream));
		when(accessClient.getDatastreamDissemination(eq(pid), eq(datastream), anyString())).thenReturn(mts);
	}

	@Test
	public void oneSourceWithMDContents() throws Exception {
		makeDatastream("/fedora/mdContents1.xml", MD_CONTENTS.getName(), source1PID);

		oneSourceTest();

		verify(managementClient).modifyDatastream(eq(source1PID), eq(MD_CONTENTS.getName()), anyString(),
				anyListOf(String.class), anyString(), anyString(), any(Document.class));
	}

	@Test
	public void oneSourceLockFailTest() throws Exception {
		makeDatastream("/fedora/mdContents1.xml", MD_CONTENTS.getName(), source1PID);

		PairedMatcher sourceRelsExtMatcher = new PairedMatcher();
		InputStream relsExtStream = this.getClass().getResourceAsStream("/fedora/containerRELSEXT1.xml");
		MIMETypedStream mts = mock(MIMETypedStream.class);
		when(mts.getHeader()).thenReturn(dsHeaders);
		when(mts.getStream()).thenReturn(IOUtils.toByteArray(relsExtStream));
		PairedAnswer sourceRelsExtAnswer = new PairedAnswer(mts, sourceRelsExtMatcher);

		// Throw locking exception on first attempt to rewrite source RELS-EXT
		doThrow(new OptimisticLockException(""))
				.doNothing()
				.when(managementClient)
				.modifyDatastream(eq(source1PID), eq(RELS_EXT.getName()), anyString(), anyListOf(String.class),
						anyString(), anyString(), argThat(sourceRelsExtMatcher));

		doThrow(new OptimisticLockException(""))
				.doNothing()
				.when(managementClient)
				.modifyDatastream(eq(destPID), eq(RELS_EXT.getName()), anyString(), anyListOf(String.class), anyString(),
						anyString(), any(Document.class));

		doThrow(new OptimisticLockException(""))
				.doNothing()
				.when(managementClient)
				.modifyDatastream(eq(source1PID), eq(MD_CONTENTS.getName()), anyString(), anyListOf(String.class),
						anyString(), anyString(), any(Document.class));

		when(accessClient.getDatastreamDissemination(eq(source1PID), eq(RELS_EXT.getName()), anyString()))
				.thenAnswer(sourceRelsExtAnswer);

		List<PID> moving = Arrays.asList(new PID("uuid:child1"), new PID("uuid:child5"));

		when(tripleStoreQueryService.fetchContainer(any(PID.class))).thenReturn(source1PID);

		digitalMan.move(moving, destPID, "user", "");

		verify(accessClient, times(3)).getDatastreamDissemination(eq(source1PID), eq(RELS_EXT.getName()), anyString());

		ArgumentCaptor<Document> sourceRelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);

		verify(managementClient, times(3)).modifyDatastream(eq(source1PID), eq(RELS_EXT.getName()), anyString(),
				anyListOf(String.class), anyString(), anyString(), sourceRelsExtUpdateCaptor.capture());

		List<Document> sourceRelsAnswers = sourceRelsExtUpdateCaptor.getAllValues();

		// Verify that the initial source RELS-EXT update is repeated after failure
		Document sourceRelsExt = sourceRelsAnswers.get(0);
		Set<PID> removed = getRelationSet(sourceRelsExt.getRootElement(), removedChild);
		assertEquals("Child tombstones should still be present", 2, removed.size());
		sourceRelsExt = sourceRelsAnswers.get(1);
		removed = getRelationSet(sourceRelsExt.getRootElement(), removedChild);
		assertEquals("Child tombstones should still be present on second try", 2, removed.size());

		// Check that tombstones were cleaned up by the end of the operation
		Document cleanRelsExt = sourceRelsAnswers.get(2);
		Set<PID> children = getRelationSet(cleanRelsExt.getRootElement(), contains);
		assertEquals("Incorrect number of children in source container after cleanup", 10, children.size());

		removed = getRelationSet(cleanRelsExt.getRootElement(), removedChild);
		assertEquals("Child tombstones not cleaned up", 0, removed.size());

		// Verify that the destination had the moved children added to it
		ArgumentCaptor<Document> destRelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);
		verify(managementClient, times(2)).modifyDatastream(eq(destPID), eq(RELS_EXT.getName()), anyString(),
				anyListOf(String.class), anyString(), anyString(), destRelsExtUpdateCaptor.capture());

		Document destRelsExt = destRelsExtUpdateCaptor.getValue();
		children = getRelationSet(destRelsExt.getRootElement(), contains);
		assertEquals("Incorrect number of children in destination container after moved", 9, children.size());
		assertTrue("Moved children were not present in destination", children.containsAll(moving));

		verify(managementClient, times(2)).modifyDatastream(eq(source1PID), eq(MD_CONTENTS.getName()),
				anyString(),
				anyListOf(String.class), anyString(), anyString(), any(Document.class));
	}

	@Test
	public void multiSourceTest() throws Exception {
		List<PID> moving = Arrays.asList(new PID("uuid:child1"), new PID("uuid:child32"));

		makeMatcherPair("/fedora/containerRELSEXT1.xml", source1PID);
		makeMatcherPair("/fedora/containerRELSEXT3.xml", source2PID);

		when(tripleStoreQueryService.fetchContainer(eq(new PID("uuid:child1")))).thenReturn(source1PID);
		when(tripleStoreQueryService.fetchContainer(eq(new PID("uuid:child32")))).thenReturn(source2PID);

		digitalMan.move(moving, destPID, "user", "");

		verify(accessClient, times(2)).getDatastreamDissemination(eq(source1PID), eq(RELS_EXT.getName()), anyString());
		verify(accessClient, times(2)).getDatastreamDissemination(eq(source2PID), eq(RELS_EXT.getName()), anyString());

		ArgumentCaptor<Document> source1RelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);
		verify(managementClient, times(2)).modifyDatastream(eq(source1PID), eq(RELS_EXT.getName()), anyString(),
				anyListOf(String.class), anyString(), anyString(), source1RelsExtUpdateCaptor.capture());

		// Check that the first source was updated
		Document clean1RelsExt = source1RelsExtUpdateCaptor.getValue();
		Set<PID> children = getRelationSet(clean1RelsExt.getRootElement(), contains);
		assertEquals("Incorrect number of children in source 1 after cleanup", 11, children.size());

		// Check that the second source was updated
		ArgumentCaptor<Document> source2RelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);
		verify(managementClient, times(2)).modifyDatastream(eq(source2PID), eq(RELS_EXT.getName()), anyString(),
				anyListOf(String.class), anyString(), anyString(), source2RelsExtUpdateCaptor.capture());
		Document clean2RelsExt = source2RelsExtUpdateCaptor.getValue();
		children = getRelationSet(clean2RelsExt.getRootElement(), contains);
		assertEquals("Incorrect number of children in source 2 after cleanup", 1, children.size());

		// Check that items from both source 1 and 2 ended up in the destination.
		ArgumentCaptor<Document> destRelsExtUpdateCaptor = ArgumentCaptor.forClass(Document.class);
		verify(managementClient).modifyDatastream(eq(destPID), eq(RELS_EXT.getName()), anyString(),
				anyListOf(String.class), anyString(), anyString(), destRelsExtUpdateCaptor.capture());

		Document destRelsExt = destRelsExtUpdateCaptor.getValue();
		children = getRelationSet(destRelsExt.getRootElement(), contains);
		assertEquals("Incorrect number of children in destination container after moved", 9, children.size());
		assertTrue("Moved children were not present in destination", children.containsAll(moving));
	}

	private Set<PID> getRelationSet(Element rdfRoot, Relationship relation) {
		List<Element> containsEls = rdfRoot.getChild("Description", JDOMNamespaceUtil.RDF_NS).getChildren(
				relation.name(), relation.getNamespace());
		Set<PID> children = new HashSet<>();
		for (Element containsEl : containsEls) {
			children.add(new PID(containsEl.getAttributeValue("resource", JDOMNamespaceUtil.RDF_NS)));
		}
		return children;
	}
}
