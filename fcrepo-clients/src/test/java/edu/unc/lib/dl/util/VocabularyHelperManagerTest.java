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
package edu.unc.lib.dl.util;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.hasVocabulary;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.replaceInvalidTerms;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.warnInvalidTerms;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.DATA_FILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.xml.VocabularyHelper;

/**
 * @author bbpennel
 * @date Oct 21, 2014
 */
public class VocabularyHelperManagerTest {

	private VocabularyHelperManager manager;

	@Mock
	private VocabularyHelper mockHelper;

	@Mock
	private MIMETypedStream dataFile;

	@Mock
	private TripleStoreQueryService queryService;
	@Mock
	private ManagementClient managementClient;
	@Mock
	private AccessClient accessClient;

	private static String COLL_PID = "info:fedora/cdr:collection";
	private static String ITEM_PID = "info:fedora/cdr:item";

	private static String VOCAB_PID = "info:fedora/cdr:vocab";
	private static String VOCAB_URI = "http://example/vocab";
	private static String VOCAB_TYPE = "vocab";

	private static PID collectionsPID = new PID("cdr:collections");

	@Before
	public void init() throws Exception {
		initMocks(this);

		manager = new VocabularyHelperManager();
		setField(manager, "queryService", queryService);
		setField(manager, "managementClient", managementClient);
		setField(manager, "accessClient", accessClient);
		setField(manager, "collectionsPID", collectionsPID);

		Map<String, Map<String, String>> vocabInfo = new HashMap<>();
		Map<String, String> entryMap = new HashMap<>();
		entryMap.put("vocabURI", VOCAB_URI);
		entryMap.put("vocabType", VOCAB_TYPE);
		vocabInfo.put(VOCAB_PID, entryMap);

		when(queryService.fetchVocabularyInfo()).thenReturn(vocabInfo);

		Map<String, Map<String, Set<String>>> vocabMapping = new HashMap<>();
		Map<String, Set<String>> collMapping = new HashMap<>();
		Set<String> levels = new HashSet<>(Arrays.asList(hasVocabulary.toString(), warnInvalidTerms.toString(),
				replaceInvalidTerms.toString()));
		collMapping.put(VOCAB_URI, levels);
		vocabMapping.put(COLL_PID, collMapping);

		when(queryService.fetchVocabularyMapping()).thenReturn(vocabMapping);

		Map<String, String> helperClassMap = new HashMap<>();
		helperClassMap.put("vocab", "edu.unc.lib.dl.util.TestVocabularyHelper");
		manager.setHelperClasses(helperClassMap);

		when(dataFile.getStream()).thenReturn(new byte[0]);
		when(accessClient.getDatastreamDissemination(any(PID.class), eq(DATA_FILE.getName()), anyString())).thenReturn(
				dataFile);

		when(queryService.fetchParentCollection(any(PID.class))).thenReturn(new PID(COLL_PID));
	}

	@Test
	public void getHelpersTest() {
		manager.init();

		Set<VocabularyHelper> helpers = manager.getHelpers(new PID(ITEM_PID));
		assertEquals("Incorrect number of helpers were found for item", helpers.size(), 1);
		assertTrue("Wrong kind of helper was returned", helpers.iterator().next() instanceof TestVocabularyHelper);

	}

	@Test
	public void getInvalidTermsTest() throws Exception {
		manager.init();

		// Add in some invalid terms
		Set<VocabularyHelper> helpers = manager.getHelpers(new PID(ITEM_PID));
		((TestVocabularyHelper) helpers.iterator().next()).setInvalidTerms(new HashSet<>(Arrays.asList("term", "term2")));

		Element doc = mock(Element.class);
		Map<String, Set<String>> invalidTerms = manager.getInvalidTerms(new PID(ITEM_PID), doc);

		assertEquals("Incorrect number of vocabularies returned", 1, invalidTerms.size());

		assertEquals("Incorrect number of invalid terms returned for vocabulary", 2, invalidTerms.get(VOCAB_URI).size());
	}

	@Test
	public void updateInvalidTermsTest() throws Exception {
		manager.init();

		Set<VocabularyHelper> helpers = manager.getHelpers(new PID(ITEM_PID));
		((TestVocabularyHelper) helpers.iterator().next()).setInvalidTerms(new HashSet<>(Arrays.asList("term", "term2")));

		when(queryService.fetchBySubjectAndPredicate(any(PID.class), anyString())).thenReturn(Arrays.asList("term"));

		Element doc = mock(Element.class);
		manager.updateInvalidTerms(new PID(ITEM_PID), doc);

		verify(managementClient, never()).purgeLiteralStatement(any(PID.class), anyString(), anyString(), anyString());

		verify(managementClient).addLiteralStatement(any(PID.class), anyString(), anyString(), anyString());
	}
}
