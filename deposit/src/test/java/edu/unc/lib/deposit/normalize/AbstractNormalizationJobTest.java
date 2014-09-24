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

import static edu.unc.lib.deposit.work.DepositGraphUtils.cdrprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.fprop;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.hasSourceMetadataProfile;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.sourceMetadata;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.hasDatastream;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.stagingLocation;
import static edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.AGGREGATE_WORK;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.jdom.Element;
import org.jdom.xpath.XPath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;

/**
 * @author bbpennel
 * @date Jun 18, 2014
 */
public abstract class AbstractNormalizationJobTest {

	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder();

	@Mock
	protected JobStatusFactory jobStatusFactory;
	@Mock
	protected DepositStatusFactory depositStatusFactory;

	protected File depositsDirectory;

	protected String jobUUID;
	protected String depositUUID;
	protected File depositDir;

	@Before
	public void initBase() throws Exception {
		initMocks(this);

		depositsDirectory = tmpFolder.newFolder("deposits");

		jobUUID = UUID.randomUUID().toString();

		depositUUID = UUID.randomUUID().toString();
		depositDir = new File(depositsDirectory, depositUUID);
		depositDir.mkdir();
	}

	protected File verifyStagingLocationExists(Resource resource, Property stagingLoc, File depositDirectory,
			String fileLabel) {
		String filePath = resource.getProperty(stagingLoc).getLiteral().getString();
		File file = new File(depositDirectory, filePath);
		assertTrue(fileLabel + " file did not exist", file.exists());

		return file;
	}

	protected void verifyMetadataSourceAssigned(Model model, Resource primaryResource, File depositDirectory,
			String sourceType, String fileSuffix) {
		Property stagingLoc = dprop(model, stagingLocation);
		Property hasSourceMetadata = cdrprop(model, hasSourceMetadataProfile);
		Property sourceMD = cdrprop(model, sourceMetadata);
		Property hasDS = dprop(model, hasDatastream);

		assertEquals("Did not have metadata source type", sourceType, primaryResource.getProperty(hasSourceMetadata)
				.getLiteral().getString());

		// Verify that the metadata source attribute is present and transitively points to the file
		Resource sourceMDResource = primaryResource.getProperty(sourceMD).getResource();
		assertNotNull("Source metdata was not assigned to main resource", sourceMDResource);

		File sourceMDFile = verifyStagingLocationExists(sourceMDResource, stagingLoc, depositDirectory,
				"Original metadata");
		assertTrue("Original metadata file did not have the correct suffix, most likely the wrong file", sourceMDFile
				.getName().endsWith(fileSuffix));

		// Verify that the extra datastream being added is the same as the source metadata
		String sourceMDDatastream = primaryResource.getProperty(hasDS).getResource().getURI();
		assertEquals("Source datastream path did not match the sourceMetadata", sourceMDResource.getURI(),
				sourceMDDatastream);
	}

	protected void copyTestPackage(String filename, AbstractDepositJob job) {
		copyTestPackage(filename, null, job);
	}

	protected void copyTestPackage(String filename, String destFilename, AbstractDepositJob job) {
		job.getDataDirectory().mkdir();
		Path packagePath = Paths.get(filename);
		try {
			Path destPath;
			if (destFilename == null) {
				destPath = job.getDataDirectory().toPath().resolve(packagePath.getFileName());
			} else {
				destPath = job.getDataDirectory().toPath().resolve(destFilename);
			}
			Files.copy(packagePath, destPath);
		} catch (Exception e) {
		}
	}

	protected boolean isAggregate(Resource resource, Model model) {
		return isContainerType(resource, AGGREGATE_WORK, model);
	}

	protected boolean isContainerType(Resource resource, ContentModelHelper.Model containerType, Model model) {
		Property hasContentModel = fprop(model, hasModel);

		StmtIterator cmIt = resource.listProperties(hasContentModel);
		boolean isSpecialized = false;
		boolean isContainer = false;
		while (cmIt.hasNext()) {
			String cmValue = cmIt.next().getResource().getURI();
			if (containerType.equals(cmValue)) {
				isSpecialized = true;
			}
			if (CONTAINER.equals(cmValue)) {
				isContainer = true;
			}
		}

		return isSpecialized && isContainer;
	}

	protected Element element(String xpathString, Object xmlObject) throws Exception {
		return (Element) xpath(xpathString, xmlObject).get(0);
	}

	protected List<?> xpath(String xpath, Object xmlObject) throws Exception {
		XPath namePath = XPath.newInstance(xpath);
		namePath.addNamespace("mods", MODS_V3_NS.getURI());
		return namePath.selectNodes(xmlObject);
	}
}
