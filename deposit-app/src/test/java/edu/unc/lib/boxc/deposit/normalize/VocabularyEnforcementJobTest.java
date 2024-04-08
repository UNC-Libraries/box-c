package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.boxc.common.xml.SecureXMLFactory;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.normalize.VocabularyEnforcementJob;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.vocab.VocabularyHelper;
import edu.unc.lib.boxc.operations.impl.vocab.VocabularyHelperManager;

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

    @BeforeEach
    public void setup() throws Exception {
        job = new VocabularyEnforcementJob();
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "vocabManager", vocabManager);
        setField(job, "depositModelManager", depositModelManager);

        PID depositPid = pidMinter.mintContentPid();
        depositStatus = new HashMap<>();
        depositStatus.put(DepositField.containerId.name(), depositPid.toString());
        when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);

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
                job.getModsPath(rescPid, true));

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
        assertTrue(testElement != null, "Document was not modified");
    }

    @Test
    public void noRewriteMODSTest() throws Exception {
        Path path = job.getModsPath(rescPid, true);
        Files.copy(Paths.get("src/test/resources/mods/singleAffiliationMods.xml"), path);
        long modified = path.toFile().lastModified();

        Set<VocabularyHelper> helpers = new HashSet<>(Arrays.asList(mockHelper));
        when(vocabManager.getRemappingHelpers(any(PID.class))).thenReturn(helpers);
        when(mockHelper.updateDocumentTerms(any(Element.class))).thenReturn(false);

        Thread.sleep(100L);

        job.run();

        verify(mockHelper).updateDocumentTerms(any(Element.class));
        assertTrue(modified == path.toFile().lastModified(), "Timestamp on MODS file changed");
    }

    private Document getMODSDocument(String uuid) throws Exception {
        File modsFile = job.getModsPath(PIDs.get(uuid)).toFile();

        SAXBuilder sb = SecureXMLFactory.createSAXBuilder();
        return sb.build(modsFile);
    }
}
