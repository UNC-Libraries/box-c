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
package edu.unc.lib.dl.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * @author bbpennel
 * @date Jun 24, 2014
 */
public class DepartmentOntologyUtilTest {

    private DepartmentOntologyUtil job;

    @Before
    public void init() throws Exception {
        job = new DepartmentOntologyUtil();
        job.setContent(Files.readAllBytes(Paths.get("src/test/resources/samples/dept-ontology.xml")));
    }

    @Test
    public void getDepartmentExactTest() {

        List<List<String>> result = job.getAuthoritativeForm("College of Arts and Sciences");

        assertEquals("Exact match did not return a result", "College of Arts and Sciences", result.get(0).get(0));
    }

    @Test
    public void getDepartmentHierarchyTest() {

        List<List<String>> result = job.getAuthoritativeForm("Folklore Program");

        assertEquals("Incorrect number of paths returned", 1, result.size());
        assertEquals("Incorrect number of path items returned", 3, result.get(0).size());
        assertEquals("Exact match did not return a result", "College of Arts and Sciences", result.get(0).get(0));
        assertEquals("Incorrect second tier", "Department of American Studies", result.get(0).get(1));
        assertEquals("Folklore Program", result.get(0).get(2));

    }

    @Test
    public void getDepartmentInvertedTest() {

        List<List<String>> result = job.getAuthoritativeForm("Nutrition Department");

        assertEquals("Incorrect number of path entries returned", 2, result.get(0).size());
        assertEquals("Gillings School of Global Public Health", result.get(0).get(0));
        assertEquals("Department of Nutrition", result.get(0).get(1));

    }

    @Test
    public void getDepartmentAltLabelTest() {

        List<List<String>> result = job.getAuthoritativeForm("School of Public Health");

        assertEquals("Incorrect number of path entries returned", 1, result.get(0).size());
        assertEquals("Gillings School of Global Public Health", result.get(0).get(0));

    }

    @Test
    public void getDepartmentWithoutPrefixTest() {

        List<List<String>> result = job.getAuthoritativeForm("Public Health");

        assertEquals("Incorrect number of path entries returned", 1, result.get(0).size());
        assertEquals("Gillings School of Global Public Health", result.get(0).get(0));

    }

    @Test
    public void getDepartmentAbbreviatedTest() {

        List<List<String>> result = job.getAuthoritativeForm("Dept of Nutrition");

        assertEquals("Incorrect number of path entries returned", 2, result.get(0).size());
        assertEquals("Gillings School of Global Public Health", result.get(0).get(0));
        assertEquals("Department of Nutrition", result.get(0).get(1));

    }

    @Test
    public void getDepartmentParenFormatTest() {

        List<List<String>> result = job.getAuthoritativeForm("Public Health (Nutrition)");

        assertEquals("Incorrect number of path entries returned", 2, result.get(0).size());
        assertEquals("Gillings School of Global Public Health", result.get(0).get(0));
        assertEquals("Department of Nutrition", result.get(0).get(1));

    }

    @Test
    public void getDepartmentColonFormatTest() {

        List<List<String>> result = job.getAuthoritativeForm("Public Health: Nutrition");

        assertEquals("Incorrect number of path entries returned", 2, result.get(0).size());
        assertEquals("Gillings School of Global Public Health", result.get(0).get(0));
        assertEquals("Department of Nutrition", result.get(0).get(1));

    }

    @Test
    public void getDepartmentParenInvalidTest() {

        List<List<String>> result = job.getAuthoritativeForm("Public Health (joint)");

        assertEquals("Incorrect number of path entries returned", 1, result.get(0).size());
        assertEquals("Gillings School of Global Public Health", result.get(0).get(0));

    }

    @Test
    public void getDepartmentParenColonFormatTest() {

        List<List<String>> result = job.getAuthoritativeForm("Public Health: Doctoral (residential)");

        assertEquals("Incorrect number of path entries returned", 1, result.get(0).size());
        assertEquals("Gillings School of Global Public Health", result.get(0).get(0));

    }

    @Test
    public void getDepartmentAddressTest() {

        List<List<String>> result = job
                .getAuthoritativeForm("Department of Nutrition, Gillings School of Global Public Health, University of North Carolina at Chapel Hill, Chapel Hill, NC, USA");

        assertEquals("Incorrect number of path entries returned", 2, result.get(0).size());
        assertEquals("Gillings School of Global Public Health", result.get(0).get(0));
        assertEquals("Department of Nutrition", result.get(0).get(1));

    }

    @Test
    public void getDepartmentAddressUnknownTopLevelTest() {

        List<List<String>> result = job
                .getAuthoritativeForm("Department of Stuff, University of North Carolina at Chapel Hill, Gillings School of Global Public Health, Chapel Hill, NC, USA");

        assertEquals("Incorrect number of path entries returned", 1, result.get(0).size());
        assertEquals("Gillings School of Global Public Health", result.get(0).get(0));

    }

    @Test
    public void getDepartmentAddressUnknownTest() {

        List<List<String>> result = job
                .getAuthoritativeForm("Department of Stuff, University of North Carolina at Chapel Hill, Chapel Hill, NC, USA");

        assertNull("Unknown department should return null", result);

    }

    @Test
    public void getDepartmentAddressMissingCommaTest() {

        List<List<String>> result = job
                .getAuthoritativeForm("Department of Nutrition University of North Carolina at Chapel Hill, Chapel Hill, NC, USA");

        assertEquals("Incorrect number of path entries returned", 2, result.get(0).size());
        assertEquals("Department of Nutrition", result.get(0).get(1));

    }

    @Test
    public void getDepartmentInvalidAddressTest() {

        List<List<String>> result = job
                .getAuthoritativeForm("Department of Chemistry, Roanoke College, Salem, VA 24153, USA");

        assertNull("No result should be returned for an address outside of UNC", result);

    }

    @Test
    public void getDepartmentAddressOtherUniTest() {

        List<List<String>> result = job.getAuthoritativeForm("Department of Nutrition, New York University");

        assertNull("No result should be returned for an address outside of UNC", result);

    }

    @Test
    public void getDepartmentAddressOtherNCTest() {

        List<List<String>> result = job
                .getAuthoritativeForm("Department of Nutrition, University of North Carolina at Charlotte");

        assertNull("No result should be returned for an address outside of UNC", result);

    }

    @Test
    public void getDepartmentExtraUNCTest() {

        List<List<String>> result = job.getAuthoritativeForm("UNC Center for European Studies");

        assertEquals("Incorrect number of paths returned", 1, result.size());
        assertEquals("Incorrect number of path items returned", 2, result.get(0).size());
        assertEquals("College of Arts and Sciences", result.get(0).get(0));
        assertEquals("Center for European Studies", result.get(0).get(1));

    }

    @Test
    public void getDepartmentMultiplePathsTest() {

        List<List<String>> result = job.getAuthoritativeForm("Joint Department of Biomedical Engineering");

        assertEquals("Incorrect number of paths returned", 2, result.size());
        assertEquals("Incorrect number of path items returned", 2, result.get(0).size());
        assertEquals("College of Arts and Sciences", result.get(0).get(0));
        assertEquals("Joint Department of Biomedical Engineering", result.get(0).get(1));

        assertEquals("Joint Department of Biomedical Engineering", result.get(1).get(0));

    }

    @Test
    public void getDepartmentMultipleDeptsTest() {

        List<List<String>> result = job.getAuthoritativeForm("Department of History and Department of Music");

        assertEquals("Incorrect number of paths returned", 2, result.size());
        assertEquals("Incorrect number of path items returned", 2, result.get(0).size());
        assertEquals("College of Arts and Sciences", result.get(0).get(0));
        assertEquals("Department of History", result.get(0).get(1));

        assertEquals("College of Arts and Sciences", result.get(0).get(0));
        assertEquals("Department of Music", result.get(1).get(1));

    }

    @Test
    public void getDepartmentMultipleTest() {

        List<List<String>> result = job.getAuthoritativeForm("Departments of History and Music");

        assertEquals("Incorrect number of paths returned", 2, result.size());
        assertEquals("Incorrect number of path items returned", 2, result.get(0).size());
        assertEquals("College of Arts and Sciences", result.get(0).get(0));
        assertEquals("Department of History", result.get(0).get(1));

        assertEquals("College of Arts and Sciences", result.get(0).get(0));
        assertEquals("Department of Music", result.get(1).get(1));

    }

    @Test
    public void getInvalidTermsTest() throws Exception {
        SAXBuilder builder = new SAXBuilder();

        InputStream modsStream = new FileInputStream(new File("src/test/resources/samples/mods.xml"));
        Document modsDoc = builder.build(modsStream);

        Set<String> invalids = job.getInvalidTerms(modsDoc.getRootElement());

        assertEquals("Did not detect invalid affiliation", 1, invalids.size());
    }
    
    @Test
    public void getDepartmentInvertedLongTest() {

        List<List<String>> result = job.getAuthoritativeForm("Nutrition, Department of");

        assertEquals("Incorrect number of path entries returned", 2, result.get(0).size());
        assertEquals("Gillings School of Global Public Health", result.get(0).get(0));
        assertEquals("Department of Nutrition", result.get(0).get(1));
        
        result = job.getAuthoritativeForm("Business School, Kenan-Flagler");
        assertEquals("Kenan-Flagler Business School", result.get(0).get(0));
    }
    
    @Test
    public void getDepartmentAmpersandSubstitution() {

        List<List<String>> result = job.getAuthoritativeForm("College of Arts & Sciences");

        assertEquals("Exact match did not return a result", "College of Arts and Sciences", result.get(0).get(0));
    }
}
