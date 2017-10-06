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

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.VocabularyHelperManager;
import edu.unc.lib.dl.xml.VocabularyHelper;

/**
 * @author bbpennel
 * @date Jun 26, 2014
 */
public class VocabularyEnforcementJobTest extends AbstractNormalizationJobTest {

    private VocabularyEnforcementJob job;

    private Map<String, String> depositStatus;

    @Mock
    private VocabularyHelperManager vocabManager;

    @Mock
    private VocabularyHelper mockHelper;

    private PID rescPid;

    @Before
    public void setup() throws Exception {
        Dataset dataset = TDBFactory.createDataset();

        job = new VocabularyEnforcementJob();
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "vocabManager", vocabManager);
        setField(job, "dataset", dataset);

        PID depositPid = pidMinter.mintContentPid();
        depositStatus = new HashMap<>();
        depositStatus.put(DepositField.containerId.name(), depositPid.toString());
        when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);

        job.getDescriptionDir().mkdir();

        Model model = job.getWritableModel();
        Bag depositBag = model.createBag(job.getDepositPID().getURI());
        rescPid = pidMinter.mintContentPid();
        Resource mainResource = model.createResource(rescPid.getURI());
        depositBag.add(mainResource);
        job.closeModel();
    }

    @Test
    public void rewriteMODSTest() throws Exception {
        Files.copy(Paths.get("src/test/resources/mods/singleAffiliationMods.xml"),
                job.getDescriptionDir().toPath().resolve(rescPid.getUUID() + ".xml"));

        Set<VocabularyHelper> helpers = new HashSet<>(Arrays.asList(mockHelper));
        when(vocabManager.getRemappingHelpers(any(PID.class))).thenReturn(helpers);

        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Element doc = (Element) invocation.getArguments()[0];
                doc.addContent(new Element("testElement"));
                return true;
            }

        }).when(mockHelper).updateDocumentTerms(any(Element.class));

        job.run();

        verify(mockHelper).updateDocumentTerms(any(Element.class));

        Document modsDoc = getMODSDocument(rescPid.getUUID());
        Element testElement = element("/mods:mods/testElement", modsDoc);
        assertTrue("Document was not modified", testElement != null);
    }

    @Test
    public void noRewriteMODSTest() throws Exception {
        Path path = job.getDescriptionDir().toPath().resolve(rescPid.getUUID() + ".xml");
        Files.copy(Paths.get("src/test/resources/mods/singleAffiliationMods.xml"), path);
        long modified = path.toFile().lastModified();

        Set<VocabularyHelper> helpers = new HashSet<>(Arrays.asList(mockHelper));
        when(vocabManager.getRemappingHelpers(any(PID.class))).thenReturn(helpers);
        when(mockHelper.updateDocumentTerms(any(Element.class))).thenReturn(false);

        Thread.sleep(100L);

        job.run();

        verify(mockHelper).updateDocumentTerms(any(Element.class));
        assertTrue("Timestamp on MODS file changed", modified == path.toFile().lastModified());
    }

    @SuppressWarnings("deprecation")
    private Document getMODSDocument(String uuid) throws Exception {
        File modsFile = new File(job.getDescriptionDir(), uuid + ".xml");

        SAXBuilder sb = new SAXBuilder(false);
        return sb.build(modsFile);
    }
}
