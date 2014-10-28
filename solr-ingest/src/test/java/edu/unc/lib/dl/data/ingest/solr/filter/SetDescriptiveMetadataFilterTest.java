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

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.xml.DepartmentOntologyUtil;

public class SetDescriptiveMetadataFilterTest {

	@Mock
	private DepartmentOntologyUtil deptUtil;

	private SetDescriptiveMetadataFilter filter;

	@Before
	public void setup() throws Exception {
		initMocks(this);

		filter = new SetDescriptiveMetadataFilter();
		setField(filter, "deptUtil", deptUtil);
	}

	@Test
	public void extractMODS() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:aggregate");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		dip.setFoxml(foxml);

		when(deptUtil.getAuthoritativeForm(anyString())).thenReturn(null);

		when(deptUtil.getAuthoritativeForm(eq("Dept of Biostatistics University of North Carolina, Chapel Hill")))
				.thenReturn(Arrays.asList(Arrays.asList("Department of Biostatistics")));

		filter.filter(dip);
		IndexDocumentBean idb = dip.getDocument();

		assertEquals("Judson, Richard", idb.getCreatorSort());
		assertEquals(4, idb.getCreator().size());
		assertEquals(5, idb.getContributor().size());
		assertEquals(1, idb.getDepartment().size());
		assertEquals("Department of Biostatistics", idb.getDepartment().get(0));

		assertNotNull(idb.getAbstractText());
		assertEquals(
				"A Comparison of Machine Learning Algorithms for Chemical Toxicity Classification Using a Simulated Multi-Scale Data Model",
				idb.getTitle());
		assertEquals(1, idb.getOtherTitle().size());

		assertEquals("BMC Bioinformatics. 2008 May 19;9(1):241", idb.getCitation());

		assertEquals("English", idb.getLanguage().get(0));
		assertEquals(DateTimeUtil.parseUTCToDate("2008-05-19T00:00:00.000Z"), idb.getDateCreated());
		assertTrue(idb.getIdentifier().contains("pmpid|18489778"));
		assertTrue(idb.getIdentifier().contains("doi|10.1186/1471-2105-9-241"));

		assertTrue(idb.getKeyword().contains("text"));
		assertTrue(idb.getKeyword().contains("Peer Reviewed"));
		assertTrue(idb.getKeyword().contains("2008"));

		assertTrue(idb.getSubject().contains("Machine Learning"));
		assertEquals(1, idb.getSubject().size());
	}

	@Test
	public void noMODS() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:37c23b03-0ca4-4487-a1c5-92c28cadc71b");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File("src/test/resources/foxml/imageNoMODS.xml")));
		dip.setFoxml(foxml);

		filter.filter(dip);
		IndexDocumentBean idb = dip.getDocument();

		assertNull(idb.getCreator());
		assertNull(idb.getContributor());
		assertNull(idb.getDepartment());
		assertNull(idb.getSubject());
		assertNull(idb.getIdentifier());
		assertNull(idb.getAbstractText());

		assertEquals(idb.getId(), dip.getPid().getPid());
		assertEquals("A1100-A800 NS final.jpg", idb.getTitle());
	}

}
