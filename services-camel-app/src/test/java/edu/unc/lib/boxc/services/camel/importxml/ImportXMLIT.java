package edu.unc.lib.boxc.services.camel.importxml;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper.newStorageLocationTestHelper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.MimeMessage;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.samskivert.mustache.Template;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.importxml.ImportXMLService;
import edu.unc.lib.boxc.operations.test.ModsTestHelper;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.impl.transfer.BinaryTransferServiceImpl;
import edu.unc.lib.boxc.services.camel.importxml.ImportXMLProcessor;

/**
 * @author bbpennel
 *
 */
@ExtendWith(SpringExtension.class)
@CamelSpringBootTest
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/jms-context.xml"),
    @ContextConfiguration("/import-xml-it-context.xml")
})
public class ImportXMLIT {

    private final static String USER_EMAIL = "user@example.com";

    private final static String UPDATED_TITLE = "Updated Work Title";
    private final static String UPDATED_DATE = "2018-04-06";

    @TempDir
    public Path tmpFolder;

    @Autowired
    private String baseAddress;

    @Autowired
    private RepositoryObjectFactory repoObjectFactory;
    @javax.annotation.Resource(name = "repositoryObjectLoader")
    private RepositoryObjectLoader repoObjectLoader;

    @Autowired
    private CamelContext cdrImportXML;

    @Autowired
    private JmsTemplate importXmlJmsTemplate;

    private ImportXMLService importXmlService;

    private StorageLocationManager locationManager;
    @Autowired
    private BinaryTransferServiceImpl binaryTransferService;
    @Autowired
    private ImportXMLProcessor importXMLProcessor;
    @Autowired
    private JavaMailSender mailSender;
    @Mock
    private MimeMessage mimeMessage;
    @Autowired
    private Template updateCompleteTemplate;

    @Mock
    private AgentPrincipals agent;

    @BeforeEach
    public void setup() throws Exception {
        initMocks(this);

        TestHelper.setContentBase(baseAddress);

        importXmlService = new ImportXMLService();
        importXmlService.setJmsTemplate(importXmlJmsTemplate);
        importXmlService.setDataDir(tmpFolder.toAbsolutePath().toString());
        importXmlService.init();

        locationManager = newStorageLocationTestHelper()
                .addTestLocation()
                .createLocationManager(repoObjectLoader);

        binaryTransferService.setStorageLocationManager(locationManager);
        importXMLProcessor.setLocationManager(locationManager);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(updateCompleteTemplate.execute(any())).thenReturn("");
    }

    @Test
    public void updateDescription() throws Exception {
        FolderObject folderObject = repoObjectFactory.createFolderObject(null);

        Document updateDoc = ModsTestHelper.makeUpdateDocument();
        ModsTestHelper.addObjectUpdate(updateDoc, folderObject.getPid(), null)
            .addContent(ModsTestHelper.modsWithTitleAndDate(UPDATED_TITLE, UPDATED_DATE));
        InputStream importStream = ModsTestHelper.documentToInputStream(updateDoc);

        importXmlService.pushJobToQueue(importStream, agent, USER_EMAIL);

        NotifyBuilder notify = new NotifyBuilder(cdrImportXML)
                .whenCompleted(1)
                .create();

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue(result, "Processing message did not match expectations");

        FolderObject updateObject = repoObjectLoader.getFolderObject(folderObject.getPid());
        Document modsDoc = parseDescription(updateObject);
        assertModsFields(modsDoc, UPDATED_TITLE, UPDATED_DATE);
    }

    private Document parseDescription(ContentObject obj) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        return builder.build(obj.getDescription().getBinaryStream());
    }

    private void assertModsFields(Document doc, String expectedTitle, String expectedDate) throws Exception {
        Element rootEl = doc.getRootElement();
        String title = rootEl.getChild("titleInfo", MODS_V3_NS).getChildText("title", MODS_V3_NS);
        String dateCreated = rootEl.getChild("originInfo", MODS_V3_NS).getChildText("dateCreated", MODS_V3_NS);
        assertEquals(expectedTitle, title);
        assertEquals(expectedDate, dateCreated);
    }
}
