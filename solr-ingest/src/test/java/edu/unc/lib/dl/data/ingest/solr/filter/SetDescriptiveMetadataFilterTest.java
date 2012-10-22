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

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Test;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.DateTimeUtil;

import junit.framework.Assert;

public class SetDescriptiveMetadataFilterTest extends Assert {
	@Test
	public void extractMODS() throws Exception {
		DocumentIndexingPackage dip = new DocumentIndexingPackage("info:fedora/uuid:aggregate");
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(new FileInputStream(new File(
				"src/test/resources/foxml/aggregateSplitDepartments.xml")));
		dip.setFoxml(foxml);

		SetDescriptiveMetadataFilter filter = new SetDescriptiveMetadataFilter();
		filter.filter(dip);
		IndexDocumentBean idb = dip.getDocument();

		assertEquals("Judson, Richard", idb.getCreatorSort());
		assertEquals(4, idb.getCreator().size());
		assertEquals(5, idb.getContributor().size());
		assertEquals(5, idb.getDepartment().size());
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

		SetDescriptiveMetadataFilter filter = new SetDescriptiveMetadataFilter();
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

	@Test
	public void splitDepts() throws Exception {
		SetDescriptiveMetadataFilter filter = new SetDescriptiveMetadataFilter();
		List<String> results;

		results = filter.splitDepartment("Dept of Biostatistics University of North Carolina, Chapel Hill");
		assertEquals("Biostatistics", results.get(0));

		results = filter
				.splitDepartment("Pulmonary Research and Treatment Center, Department of Medicine, University of North Carolina at Chapel Hill");
		assertEquals("Medicine", results.get(0));

		results = filter
				.splitDepartment("Pulmonary Toxicology Branch, Experimental Toxicology Division, National Health and Environmental Effects Research Laboratory, Office of Research and Development, United States Environmental Protection Agency");
		assertEquals("United States Environmental Protection Agency", results.get(0));

		results = filter
				.splitDepartment("Robert Wood Johnson Clinical Scholars Program and Department of Obstetrics/Gynecology");
		assertEquals("Robert Wood Johnson Clinical Scholars Program", results.get(0));
		assertEquals("Obstetrics/Gynecology", results.get(1));

		results = filter
				.splitDepartment("Program in Molecular Biology and Biotechnology, University of North Carolina, Chapel Hill");
		assertEquals("Molecular Biology and Biotechnology", results.get(0));

		results = filter.splitDepartment("School of Information and Library Science");
		assertEquals("Information and Library Science", results.get(0));

		results = filter
				.splitDepartment("Thurston Arthritis Research Center, University of North Carolina at Chapel Hill");
		assertEquals("Thurston Arthritis Research Center", results.get(0));

		results = filter.splitDepartment("Molecular Staging Inc., 300 George St., New Haven, CT 06511 USA");
		assertNull(results);

		results = filter.splitDepartment("Department of Health Policy and Administration, School of Public Health");
		assertEquals("Health Policy and Administration", results.get(0));

		results = filter.splitDepartment("Department of Microbiology & Immunology");
		assertEquals("Microbiology and Immunology", results.get(0));

		results = filter
				.splitDepartment("Center of Excellence in Epidemiology, Biostatistics, and Disease Prevention, Mount Sinai School of Medicine, One Gustave L. Levy Place, Box 1057, New York, New York, USA");
		assertNull(results);

		results = filter
				.splitDepartment("Lineberger Comprehensive Cancer Center, University of North Carolina at Chapel Hill");
		assertEquals("Lineberger Comprehensive Cancer Center", results.get(0));

		results = filter.splitDepartment("Current address: Department of Environmental Sciences and Engineering");
		assertEquals("Environmental Sciences and Engineering", results.get(0));

		// Discarded because it looks like an outside location.
		results = filter
				.splitDepartment("Departments of Medicine and Epidemiology, Johns Hopkins School of Medicine and Johns Hopkins Bloomberg School of Public Health, Baltimore, USA");
		assertNull(results);

		// False positives since it can't figure out this isn't a UNC department
		results = filter
				.splitDepartment("Department of Medicine and Department of Epidemiology, Section of Stuff, Johns Hopkins School of Medicine and Johns Hopkins Bloomberg School of Public Health");
		assertEquals("Medicine", results.get(0));
		assertEquals("Epidemiology", results.get(1));
		
		results = filter
				.splitDepartment("Cancer Genetics Program, Lineburger Comprehensive Cancer Center, University of North Carolina at Chapel Hill");
		System.out.println(results.get(0));
		

//		BufferedReader br = new BufferedReader(new FileReader("src/test/resources/departments.txt"));
//		String line = null;
//		System.out.println("<html><body><style>td {border-top: 1px solid #ddd;}</style><table>");
//		while ((line = br.readLine()) != null) {
//			List<String> departments = filter.splitDepartment(line);
//
//			if (departments == null) {
//				System.out.println("<tr><td width='35%'>" + line + "</td><td width='33%'>-</td><td>-</td></tr>");
//			} else {
//				boolean first = true;
//				for (String department : departments) {
//					if (first) {
//						first = false;
//						System.out.print("<tr><td width='35%'>" + line + "</td>");
//					} else {
//						System.out.print("<tr><td width='35%'></td>");
//					}
//					System.out.print("<td width='33%'>" + department + "</td>");
//					if (filter.departmentInVocabulary(department)) {
//						System.out.print("<td>" + department + "</td>");
//					} else {
//						System.out.print("<td>-</td>");
//					}
//					System.out.println("</tr>");
//				}
//			}
//		}
//		br.close();
//		System.out.println("</table></body></html>");

	}
}
