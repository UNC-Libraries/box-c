package edu.unc.lib.boxc.deposit.normalize;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.Resource;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.boxc.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * @author bbpennel
 * @date Jun 18, 2014
 */
public abstract class AbstractNormalizationJobTest extends AbstractDepositJobTest {

    @BeforeEach
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
        assertTrue(file.exists(), fileLabel + " file did not exist");

        return file;
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
