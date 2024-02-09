package edu.unc.lib.boxc.operations.impl.edit;

import static edu.unc.lib.boxc.model.api.DatastreamType.MD_DESCRIPTIVE;
import static edu.unc.lib.boxc.model.api.DatastreamType.MD_DESCRIPTIVE_HISTORY;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.DCR_PACKAGING_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.boxc.operations.test.ModsTestHelper.documentToInputStream;
import static edu.unc.lib.boxc.operations.test.ModsTestHelper.modsWithTitleAndDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import edu.unc.lib.boxc.operations.api.exceptions.StateUnmodifiedException;
import edu.unc.lib.boxc.operations.impl.versioning.DatastreamHistoryLog;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;

/**
 * @author bbpennel
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/import-job-it.xml")
})
public class UpdateDescriptionServiceIT {
    private AutoCloseable closeable;

    @Autowired
    private String baseAddress;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;

    @Autowired
    private UpdateDescriptionService updateService;

    @Mock
    private AgentPrincipals agent;

    @BeforeEach
    public void init_() throws Exception {
        closeable = openMocks(this);

        TestHelper.setContentBase(baseAddress);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void addDescriptionToWork() throws Exception {
        WorkObject workObj = repoObjFactory.createWorkObject(null);

        addDescription(workObj, "new title", "2018-04-06");

        List<BinaryObject> mdBins = workObj.listMetadata();
        assertEquals(1, mdBins.size());
        BinaryObject modsBin = mdBins.get(0);
        assertEquals(DatastreamPids.getMdDescriptivePid(workObj.getPid()), modsBin.getPid());
        assertEquals(MD_DESCRIPTIVE.getDefaultFilename(), modsBin.getFilename());

        assertHasMods(modsBin.getBinaryStream(), "new title", "2018-04-06");
    }

    @Test
    public void updateDescriptionOnFolder() throws Exception {
        FolderObject folderObj = repoObjFactory.createFolderObject(null);

        addDescription(folderObj, "new title", "2018-04-06");
        addDescription(folderObj, "updated title", "2018-04-08");

        List<BinaryObject> mdBins = folderObj.listMetadata();
        assertEquals(2, mdBins.size());

        BinaryObject modsBin = findDatastream(mdBins, MD_DESCRIPTIVE);
        assertEquals(DatastreamPids.getMdDescriptivePid(folderObj.getPid()), modsBin.getPid());
        assertEquals(MD_DESCRIPTIVE.getDefaultFilename(), modsBin.getFilename());

        assertHasMods(modsBin.getBinaryStream(), "updated title", "2018-04-08");

        // Make sure the history gets added
        assertNotNull(findDatastream(mdBins, MD_DESCRIPTIVE_HISTORY));
    }

    @Test
    public void updateDescriptionSkipUnmodified() throws Exception {
        FolderObject folderObj = repoObjFactory.createFolderObject(null);

        addDescription(folderObj, "new title", "2018-04-06");
        // Perform a second update with the exact same content
        assertThrows(StateUnmodifiedException.class, () -> {
            addDescription(folderObj, "new title", "2018-04-06");
        });

        // Update should have been skipped, so no history datastream
        List<BinaryObject> mdBins = folderObj.listMetadata();
        assertEquals(1, mdBins.size());
        BinaryObject modsBin = findDatastream(mdBins, MD_DESCRIPTIVE);
        assertHasMods(modsBin.getBinaryStream(), "new title", "2018-04-06");
        assertNull(findDatastream(mdBins, MD_DESCRIPTIVE_HISTORY));
    }

    @Test
    public void updateDescriptionSkipUnmodifiedWithHistory() throws Exception {
        FolderObject folderObj = repoObjFactory.createFolderObject(null);

        addDescription(folderObj, "new title", "2018-04-06");
        addDescription(folderObj, "updated title", "2018-04-08");
        // Perform a third update with the exact same content
        assertThrows(StateUnmodifiedException.class, () -> {
            addDescription(folderObj, "updated title", "2018-04-08");
        });

        // History should be present, but only have one entry in it
        List<BinaryObject> mdBins = folderObj.listMetadata();
        assertEquals(2, mdBins.size());
        BinaryObject modsBin = findDatastream(mdBins, MD_DESCRIPTIVE);
        assertHasMods(modsBin.getBinaryStream(), "updated title", "2018-04-08");
        var historyDs = findDatastream(mdBins, MD_DESCRIPTIVE_HISTORY);
        assertNotNull(historyDs);
        var rootEl = deserializeXml(historyDs.getBinaryStream());
        var versionEls = rootEl.getChildren(DatastreamHistoryLog.VERSION_TAG, DCR_PACKAGING_NS);
        assertEquals(1, versionEls.size());
    }

    private BinaryObject findDatastream(List<BinaryObject> mdBins, DatastreamType dsType) {
        return mdBins.stream()
                .filter(bin -> bin.getPid().getComponentId().endsWith(dsType.getId()))
                .findFirst()
                .orElse(null);
    }

    private void addDescription(ContentObject contentObj, String title, String date) throws Exception {
        Document doc = new Document()
                .addContent(modsWithTitleAndDate(title, date));
        InputStream modsStream = documentToInputStream(doc);
        updateService.updateDescription(new UpdateDescriptionRequest(agent, contentObj, modsStream));
    }

    private void assertHasMods(InputStream updatedMods, String expectedTitle, String expectedDate)
            throws JDOMException, IOException {
        Element rootEl = deserializeXml(updatedMods);
        String title = rootEl.getChild("titleInfo", MODS_V3_NS).getChildText("title", MODS_V3_NS);
        String dateCreated = rootEl.getChild("originInfo", MODS_V3_NS).getChildText("dateCreated", MODS_V3_NS);
        assertEquals(expectedTitle, title);
        assertEquals(expectedDate, dateCreated);
    }

    private Element deserializeXml(InputStream xmlStream) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(xmlStream);
        return doc.getRootElement();
    }
}