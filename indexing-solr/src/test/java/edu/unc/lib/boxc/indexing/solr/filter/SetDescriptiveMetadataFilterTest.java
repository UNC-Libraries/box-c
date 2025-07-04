package edu.unc.lib.boxc.indexing.solr.filter;

import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makeFileObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.search.solr.services.TitleRetrievalService;
import edu.unc.lib.boxc.search.solr.utils.DateFormatUtil;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public class SetDescriptiveMetadataFilterTest {
    private static final String PID_STRING = "uuid:07d9594f-310d-4095-ab67-79a1056e7430";

    private AutoCloseable closeable;

    @Mock
    private DocumentIndexingPackageDataLoader loader;
    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private TitleRetrievalService titleRetrievalService;

    private IndexDocumentBean idb;
    @Mock
    private ContentObject contentObj;
    @Mock
    private Resource objResc;

    @Mock
    private PID pid;

    private SetDescriptiveMetadataFilter filter;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    @Captor
    private ArgumentCaptor<Date> dateCaptor;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        when(pid.getId()).thenReturn(PID_STRING);

        idb = new IndexDocumentBean();
        idb.setTitle("Title");
        idb.setDateAdded(String.valueOf(LocalDateTime.now()));

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);
        when(dip.getContentObject()).thenReturn(contentObj);
        when(contentObj.getResource()).thenReturn(objResc);

        filter = new SetDescriptiveMetadataFilter();
        filter.setTitleRetrievalService(titleRetrievalService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testInventory() throws Exception {
        runFilterOnInventoryMods();

        assertEquals("Paper title", idb.getTitle());
        assertNull(idb.getOtherTitle());

        List<String> creators = idb.getCreator();
        assertTrue(creators.contains("Test, author"));
        assertTrue(creators.contains("Test, author2"));
        assertTrue(creators.contains("Gilmer, Jeremy Francis, 1918-2020"));
        assertTrue(creators.contains("Boxy, Berry Jean, Jr., 1991-"));

        List<String> contributors = idb.getContributor();
        assertTrue(contributors.contains("Boxy, Alice, III, 1994-"));
        assertTrue(contributors.contains("Test, contributor"));
        assertTrue(contributors.contains("Boxy, Assistant"));
        assertTrue(contributors.contains("Boxy, Role Free"));

        List<String> creatorContributors = idb.getCreatorContributor();
        assertTrue(creatorContributors.contains("Test, author"));
        assertTrue(creatorContributors.contains("Test, author2"));
        assertTrue(creatorContributors.contains("Gilmer, Jeremy Francis, 1918-2020"));
        assertTrue(creatorContributors.contains("Boxy, Berry Jean, Jr., 1991-"));
        assertTrue(creatorContributors.contains("Boxy, Alice, III, 1994-"));
        assertTrue(creatorContributors.contains("Test, contributor"));
        assertTrue(creatorContributors.contains("Boxy, Assistant"));
        assertTrue(creatorContributors.contains("Boxy, Role Free"));

        assertEquals("Abstract text", idb.getAbstractText());

        assertEquals("40148", idb.getCollectionId());
        assertEquals("image0625rb", idb.getHookId());

        assertTrue(idb.getSubject().contains("Test resource"));
        assertTrue(idb.getSubject().contains("Test resource two"));
        assertTrue(idb.getSubject().contains("rules, boxy"));
        assertFalse(idb.getSubject().contains("Subject Title"));
        assertFalse(idb.getSubject().contains("Germany"));

        assertTrue(idb.getLocation().contains("Germany"));
        assertTrue(idb.getLocation().contains("Canada"));

        assertTrue(idb.getLanguage().contains("Provençal, Old (to 1500);Occitan, Old (to 1500)"));

        assertTrue(idb.getPublisher().contains("Knopf"));
        assertEquals(1, idb.getPublisher().size());

        assertEquals("2006-04-01T00:00:00.000Z", DateFormatUtil.formatter.format(idb.getDateCreated()));
        assertEquals("2006", idb.getDateCreatedYear());

        assertTrue(idb.getOtherSubject().contains("Germany"));
        assertTrue(idb.getOtherSubject().contains("Canada"));
        assertTrue(idb.getOtherSubject().contains("Explorer"));
        assertTrue(idb.getOtherSubject().contains("scale"));
        assertTrue(idb.getOtherSubject().contains("Subject Title"));
        assertEquals(5, idb.getOtherSubject().size());

        List<String> ids = idb.getIdentifier();
        assertTrue(ids.contains("local|abc123"));
        assertFalse(ids.contains("uri|http://example.com"));

        List<String> keywords = idb.getKeyword();
        assertTrue(keywords.contains("abc123"));

        assertFalse(keywords.contains("Dissertation"));
        assertTrue(keywords.contains("text"));
        assertTrue(keywords.contains("note"));
        assertTrue(keywords.contains("phys note"));
        assertTrue(keywords.contains("Cited source"));

        assertTrue(idb.getGenre().contains("Dissertation"));
        assertTrue(idb.getGenre().contains("Paper"));
        assertTrue(idb.getGenre().contains("Letter"));
        assertEquals(3, idb.getGenre().size());

        assertEquals("citation text", idb.getCitation());

        List<String> exhibits = idb.getExhibit();
        assertEquals(2, exhibits.size());
        assertTrue(exhibits.contains("Wonderful Exhibit|https://digital-exhibit.lib.unc.edu"));
        assertTrue(exhibits.contains("https://no-url-label-digital-exhibit.lib.unc.edu|https://no-url-label-digital-exhibit.lib.unc.edu"));
    }

    private void runFilterOnInventoryMods() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(Files.newInputStream(
                Paths.get("src/test/resources/datastream/inventoryMods.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);
    }

    @Test
    public void testRightsFromInventory() throws Exception {
        runFilterOnInventoryMods();

        assertTrue(idb.getRights().contains("Copyright Not Evaluated"));
        assertTrue(idb.getRights().contains("For copyright information or permissions questions, see our " +
                "intellectual property statement https://library.unc.edu/wilson/research/perm/"));
        assertTrue(idb.getRights().contains("Copyright Not Evaluated"));
        assertTrue(idb.getRights().contains("More Random Rights"));
        assertEquals(4, idb.getRights().size(), "Incorrect number of rights: " + idb.getRights());
    }

    @Test
    public void testRightsUriFromInventory() throws Exception {
        runFilterOnInventoryMods();

        assertTrue(idb.getRightsUri().contains("https://rightsstatements.org/vocab/CNE/1.0/"));
        assertTrue(idb.getRightsUri().contains("https://creativecommons.org/licenses/by-sa/3.0/us/"));
        assertEquals(2, idb.getRightsUri().size(), "Incorrect number of rightsUris: " + idb.getRightsUri());
    }

    @Test
    public void testRightsOaiPmhFromInventory() throws Exception {
        runFilterOnInventoryMods();

        assertTrue(idb.getRightsOaiPmh().contains("http://rightsstatements.org/vocab/CNE/1.0/"));
        assertTrue(idb.getRightsOaiPmh().contains("http://creativecommons.org/licenses/by-sa/3.0/us/"));
        assertTrue(idb.getRightsOaiPmh().contains("Copyright Not Evaluated"));
        assertTrue(idb.getRightsOaiPmh().contains("Attribution-ShareAlike 3.0 United States (CC BY-SA 3.0 US)"));
        assertTrue(idb.getRightsOaiPmh().contains("More Random Rights"));
        assertTrue(idb.getRightsOaiPmh().contains("For copyright information or permissions questions, see our intellectual property statement https://library.unc.edu/wilson/research/perm/"));
        assertEquals(6, idb.getRightsOaiPmh().size(), "Incorrect number of rightsOaiPmh: " + idb.getRightsOaiPmh());
    }

    /*
     * Covers case when there is not a dateCreated, but there are both dateIssued and dateCaptured fields
     */
    @Test
    public void testDateIssuedPreference() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/dateIssued.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertEquals("2006-05-01T00:00:00.000Z", DateFormatUtil.formatter.format(idb.getDateCreated()));
        assertEquals("2006", idb.getDateCreatedYear());
    }

    /*
     * Covers case when there is only a dateCaptured field
     */
    @Test
    public void testDateCapturedPreference() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/dateCaptured.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertEquals("2006-03-01T00:00:00.000Z", DateFormatUtil.formatter.format(idb.getDateCreated()));
        assertNull(idb.getDateCreatedYear());
    }

    @Test
    public void testNamePartConcatenation() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/nameParts.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        // Don't index displayForm tags
        assertFalse((idb.getCreator().contains("Boxy, Ruler of Digital Collections")));

        assertTrue(idb.getCreator().contains("Repo, Boxy"));
        assertTrue(idb.getCreator().contains("Repo2, Boxy"));
        assertTrue(idb.getCreator().contains("Test, author"));
        assertTrue(idb.getCreator().contains("Gilmer, Jeremy Francis, 1918-2020"));
        assertTrue(idb.getCreator().contains("Boxy, Berry Jean, Jr., 1991-"));
        assertTrue(idb.getCreator().contains("Given"));
        assertEquals(6, idb.getCreator().size());
        assertTrue(idb.getContributor().contains("Boxy, Alice, III, 1994-"));
        assertTrue(idb.getContributor().contains("Boxy, Assistant"));
        assertTrue(idb.getContributor().contains("Boxy, Role Free"));
        assertTrue(idb.getContributor().contains("Boxy, Role Empty"));
        assertTrue(idb.getContributor().contains("Boxy"));
        assertEquals(5, idb.getContributor().size());
    }

    @Test
    public void testMultipleCreators() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/multipleCreators.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertTrue(idb.getCreator().contains("Test, Creator1"));
        assertTrue(idb.getCreator().contains("Test, Creator2"));

        assertTrue(idb.getCreatorContributor().contains("Test, Creator1"));
        assertTrue(idb.getCreatorContributor().contains("Test, Creator2"));
    }

    @Test
    public void testInvalidLanguageCode() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/invalidLanguage.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertNull(idb.getLanguage());
    }

    @Test
    public void testMultipleLanguages() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/inventoryMods.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertTrue(idb.getLanguage().contains("Provençal, Old (to 1500);Occitan, Old (to 1500)"));
        assertTrue(idb.getLanguage().contains("Cherokee"));
    }

    @Test
    public void noMODS() {
        Date dateAdded = new Date();
        idb.setDateAdded(dateAdded);

        idb.setTitle(null);

        when(objResc.hasProperty(DcElements.title)).thenReturn(true);
        Statement titleStmt = mock(Statement.class);
        when(titleStmt.getString()).thenReturn("test label");
        when(objResc.getProperty(DcElements.title)).thenReturn(titleStmt);

        filter.filter(dip);

        assertNull(idb.getAbstractText());
        assertNull(idb.getLanguage());
        assertNull(idb.getSubject());
        assertNull(idb.getCitation());
        assertNull(idb.getIdentifier());

        // check that title, keyword and date created still get set
        assertEquals("test label", idb.getTitle());
        assertNull(idb.getDateCreated());
        assertTrue(idb.getKeyword().contains(PID_STRING));
    }

    @Test
    public void dcTitle() {
        idb.setTitle(null);

        when(objResc.hasProperty(DcElements.title)).thenReturn(true);
        Statement titleStmt = mock(Statement.class);
        when(titleStmt.getString()).thenReturn("test DC label");
        when(objResc.getProperty(DcElements.title)).thenReturn(titleStmt);

        filter.filter(dip);

        assertEquals("test DC label", idb.getTitle());
    }

    @Test
    public void ebucoreTitle() {
        idb.setTitle(null);

        when(objResc.hasProperty(DcElements.title)).thenReturn(false);
        when(objResc.hasProperty(Ebucore.filename)).thenReturn(true);
        Statement titleStmt = mock(Statement.class);
        when(titleStmt.getString()).thenReturn("test Ebucore label");
        when(objResc.getProperty(Ebucore.filename)).thenReturn(titleStmt);

        filter.filter(dip);

        assertEquals("test Ebucore label", idb.getTitle());
    }

    @Test
    public void fileTitle() {
        idb.setTitle(null);

        when(objResc.hasProperty(DcElements.title)).thenReturn(false);
        when(objResc.hasProperty(Ebucore.filename)).thenReturn(false);
        when(pid.getId()).thenReturn("uuid: 1234");

        filter.filter(dip);

        assertEquals("uuid: 1234", idb.getTitle());
    }

    @Test
    public void fileUsingFilenameAsTitle() {
        var fileObj = makeFileObject(pid, null);
        when(dip.getContentObject()).thenReturn(fileObj);
        var binObj = mock(BinaryObject.class);
        when(binObj.getFilename()).thenReturn("bin_filename.txt");
        when(fileObj.getOriginalFile()).thenReturn(binObj);
        idb.setTitle(null);

        filter.filter(dip);

        assertEquals("bin_filename.txt", idb.getTitle());
    }

    @Test
    public void streamingFileTitleWithNoTitle() {
        var fileObj = makeFileObject(pid, null);
        when(dip.getContentObject()).thenReturn(fileObj);
        doThrow(NotFoundException.class).when(fileObj).getOriginalFile();
        idb.setTitle(null);

        filter.filter(dip);

        assertEquals(pid.getId(), idb.getTitle());
    }

    @Test
    public void testDateCreatedYearBxc3941() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document modsDoc = builder.build(new FileInputStream(new File(
                "src/test/resources/datastream/dateCreatedYearIssue.xml")));
        when(dip.getMods()).thenReturn(modsDoc.detachRootElement());

        filter.filter(dip);

        assertEquals("1862-01-01T00:00:00.000Z", DateFormatUtil.formatter.format(idb.getDateCreated()));
        assertEquals("1862", idb.getDateCreatedYear());

    }
}
