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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * @author bbpennel
 * @date Jun 18, 2014
 */
public abstract class AbstractNormalizationJobTest extends AbstractDepositJobTest {

    @Before
    public void initNorm() throws Exception {
        String pidString =  UUID.randomUUID().toString();
        PID premisEventPid = PIDs.get(pidString);
        when(pidMinter.mintPremisEventPid(any(PID.class))).thenReturn(premisEventPid);
        Answer<PID> answer = new Answer<PID>() {
            @Override
            public PID answer(InvocationOnMock invocation) throws Throwable {
                return PIDs.get(UUID.randomUUID().toString());
            }
        };
        when(pidMinter.mintContentPid()).thenAnswer(answer);
    }

    protected File verifyStagingLocationExists(Resource resource, File depositDirectory,
            String fileLabel) {
        String filePath = resource.getProperty(CdrDeposit.stagingLocation).getLiteral().getString();
        File file = new File(depositDirectory, filePath);
        assertTrue(fileLabel + " file did not exist", file.exists());

        return file;
    }

    protected void verifyMetadataSourceAssigned(Model model, Resource primaryResource, File depositDirectory,
            String sourceType, String fileSuffix) {

        assertEquals("Did not have metadata source type", sourceType, primaryResource.getProperty(Cdr.hasSourceMetadataProfile)
                .getLiteral().getString());

        // Verify that the metadata source attribute is present and transitively points to the file
        Resource sourceMDResource = primaryResource.getProperty(CdrDeposit.hasSourceMetadata).getResource();
        assertNotNull("Source metdata was not assigned to main resource", sourceMDResource);

        File sourceMDFile = verifyStagingLocationExists(sourceMDResource, depositDirectory,
                "Original metadata");
        assertTrue("Original metadata file did not have the correct suffix, most likely the wrong file", sourceMDFile
                .getName().endsWith(fileSuffix));

        // Verify that the extra datastream being added is the same as the source metadata
        String sourceMDDatastream = primaryResource.getProperty(CdrDeposit.hasDatastream).getResource().getURI();
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

    protected Element element(String xpathString, Object xmlObject) throws Exception {
        return (Element) xpath(xpathString, xmlObject).get(0);
    }

    protected List<?> xpath(String xpath, Object xmlObject) throws Exception {
        org.jdom2.xpath.XPathFactory xpfac = org.jdom2.xpath.XPathFactory.instance();
        org.jdom2.xpath.XPathExpression<Content> xpe = xpfac.compile(xpath, Filters.content(), null,
                JDOMNamespaceUtil.MODS_V3_NS);
        return xpe.evaluate(xmlObject);
    }
}
