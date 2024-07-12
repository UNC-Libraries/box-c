package edu.unc.lib.boxc.services.camel.importxml;

import com.samskivert.mustache.Template;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.model.fcrepo.test.TestRepositoryDeinitializer;
import edu.unc.lib.boxc.operations.impl.importxml.ImportXMLService;
import edu.unc.lib.boxc.operations.test.ModsTestHelper;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.impl.transfer.BinaryTransferServiceImpl;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.fcrepo.client.FcrepoClient;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper.newStorageLocationTestHelper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 *
 */
public class ImportXMLIT extends CamelSpringTestSupport {
    private final static String USER_EMAIL = "user@example.com";
    private final static String UPDATED_TITLE = "Updated Work Title";
    private final static String UPDATED_DATE = "2018-04-06";

    private AutoCloseable closeable;

    @TempDir
    public Path tmpFolder;

    private String baseAddress;

    private RepositoryObjectFactory repoObjectFactory;
    private RepositoryObjectLoader repoObjectLoader;

    private CamelContext cdrImportXML;

    private JmsTemplate importXmlJmsTemplate;

    private ImportXMLService importXmlService;

    private StorageLocationManager locationManager;
    private BinaryTransferServiceImpl binaryTransferService;
    private ImportXMLProcessor importXMLProcessor;
    private JavaMailSender mailSender;
    @Mock
    private MimeMessage mimeMessage;
    private Template updateCompleteTemplate;
    private FcrepoClient fcrepoClient;

    @Mock
    private AgentPrincipals agent;

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("spring-test/cdr-client-container.xml",
                "spring-test/jms-context.xml",
                "import-xml-it-context.xml");
    }

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        baseAddress = applicationContext.getBean("baseAddress", String.class);
        fcrepoClient = applicationContext.getBean(FcrepoClient.class);
        repoObjectFactory = applicationContext.getBean(RepositoryObjectFactory.class);
        repoObjectLoader = applicationContext.getBean("repositoryObjectLoader", RepositoryObjectLoader.class);
        cdrImportXML = applicationContext.getBean("cdrImportXML", CamelContext.class);
        importXmlJmsTemplate = applicationContext.getBean("importXmlJmsTemplate", JmsTemplate.class);
        importXMLProcessor = applicationContext.getBean(ImportXMLProcessor.class);
        mailSender = applicationContext.getBean(JavaMailSender.class);
        binaryTransferService = applicationContext.getBean(BinaryTransferServiceImpl.class);
        updateCompleteTemplate = applicationContext.getBean("updateCompleteTemplate", Template.class);

        TestHelper.setContentBase(baseAddress);

        importXmlService = new ImportXMLService();
        importXmlService.setJmsTemplate(importXmlJmsTemplate);
        importXmlService.setDataDir(tmpFolder.toString());
        importXmlService.init();

        locationManager = newStorageLocationTestHelper()
                .addTestLocation()
                .createLocationManager(repoObjectLoader);

        binaryTransferService.setStorageLocationManager(locationManager);
        importXMLProcessor.setLocationManager(locationManager);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(updateCompleteTemplate.execute(any())).thenReturn("");
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
        TestRepositoryDeinitializer.cleanup(fcrepoClient);
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
        assertTrue("Processing message did not match expectations", result);

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
