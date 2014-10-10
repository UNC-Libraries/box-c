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
package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPath;
import org.junit.Before;
import org.junit.Test;


/**
 * @author bbpennel
 * @date Jun 18, 2014
 */
public class BioMedArticleHelperTest {

	private BioMedArticleHelper helper;

	@Before
	public void init() {
		helper = new BioMedArticleHelper();
	}

	@Test
	public void longSupplTitleTest() throws Exception {

		Document articleDoc = getArticleDocument("src/test/resources/biomed-simple.xml");

		Map<String, String> supplLabels = helper.getFilesLC2SupplementLabels(articleDoc);

		assertNotNull(supplLabels);
		assertEquals("Incorrect number of supplemental labels", 2, supplLabels.size());

		assertEquals("Supplemental title should have been truncated to max length",
				BioMedArticleHelper.MAX_SUPPL_TITLE_LENGTH, supplLabels.get("long.txt").length());

		assertEquals("Short title should be unchanged", 5, supplLabels.get("short.txt").length());
	}

	@Test
	public void extractModsTest() throws Exception {
		Document articleDoc = getArticleDocument("src/test/resources/biomed-simple.xml");

		Document mods = helper.extractMODS(articleDoc, null);

		List<?> titles = xpath("mods:mods/mods:titleInfo/mods:title", mods);
		assertEquals("Titles not extracted via helper", 0, titles.size());

		assertEquals("Pubmed id was not assigned correctly", "pmpid-1234",
				element("mods:mods/mods:identifier[@type='pmpid']", mods).getText());

		assertEquals("doi was not assigned correctly", "doi-1234",
				element("mods:mods/mods:identifier[@type='doi']", mods).getText());

		Element relatedItem = element("mods:mods/mods:relatedItem", mods);
		assertNotNull("Related item was not assigned from source info", relatedItem);
		assertTrue("Related item was not populated", relatedItem.getChildren().size() > 0);

		Element authorName = element("mods:mods/mods:name", mods);
		assertNotNull("No author name was extracted", authorName);
		assertEquals("Wik, Pedia I", authorName.getChild("namePart", MODS_V3_NS).getText());
		assertEquals("Wikipedia", authorName.getChild("affiliation", MODS_V3_NS).getText());
	}

	@Test
	public void extractModsExistingTest() throws Exception {
		Document startingMods = new Document();
		Element modsEl = new Element("mods", MODS_V3_NS);
		startingMods.setRootElement(modsEl);

		Element titleInfoEl = new Element("titleInfo", MODS_V3_NS);
		Element titleEl = new Element("title", MODS_V3_NS);
		titleEl.setText("The Title");
		titleInfoEl.addContent(titleEl);
		modsEl.addContent(titleInfoEl);

		Element nameEl = new Element("name", MODS_V3_NS);
		Element namePartEl = new Element("namePart", MODS_V3_NS);
		namePartEl.setText("Name, A");
		nameEl.addContent(namePartEl);
		modsEl.addContent(nameEl);

		Document articleDoc = getArticleDocument("src/test/resources/biomed-simple.xml");

		Document mods = helper.extractMODS(articleDoc, startingMods);

		assertEquals("Title should have been retained from starting document", "The Title",
				element("mods:mods/mods:titleInfo/mods:title", mods).getText());

		// Check that the author in the original document has not made it through
		List<?> authors = xpath("mods:mods/mods:name", mods);
		assertEquals("Only one author should have been retained", 1, authors.size());

		Element authorName = (Element) authors.get(0);
		assertEquals("Wik, Pedia I", authorName.getChild("namePart", MODS_V3_NS).getText());
		assertEquals("Wikipedia", authorName.getChild("affiliation", MODS_V3_NS).getText());
	}

	private Document getArticleDocument(String path) throws Exception {
		File articleFile = new File(path);

		SAXBuilder sb = new SAXBuilder(false);
		sb.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		sb.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		return sb.build(articleFile);
	}

	private Element element(String xpathString, Object xmlObject) throws Exception {
		return (Element) xpath(xpathString, xmlObject).get(0);
	}

	private List<?> xpath(String xpath, Object xmlObject) throws Exception {
		XPath namePath = XPath.newInstance(xpath);
		namePath.addNamespace("mods", MODS_V3_NS.getURI());
		return namePath.selectNodes(xmlObject);
	}
}
