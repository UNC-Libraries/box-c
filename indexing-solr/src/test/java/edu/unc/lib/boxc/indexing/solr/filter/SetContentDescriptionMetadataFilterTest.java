package edu.unc.lib.boxc.indexing.solr.filter;

import com.fasterxml.jackson.databind.JsonNode;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.utils.MachineGeneratedContentService;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class SetContentDescriptionMetadataFilterTest {
    private static final String DEFAULT_JSON_PATH =
            "src/test/resources/datastream/machineGeneratedDescriptionDefaults.json";
    private static final String ALT_TEXT_FROM_MG = "Mountain landscape with snow-covered peaks";
    private static final String ALT_TEXT_FROM_REPO = "Repo-stored alt text";
    private static final String FULL_DESC_FROM_MG = "A whole host of mountains in Asia";
    private static final String FULL_DESC_FROM_REPO = "Repo-stored full description";
    private static final String TRANSCRIPT_FROM_MG = "A beautiful mountain range";
    private static final String TRANSCRIPT_FROM_REPO = "Repo-stored transcript";

    private AutoCloseable closeable;

    @TempDir
    public Path tempDir;

    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private IndexDocumentBean idb;
    @Mock
    private FileObject fileObj;
    @Mock
    private WorkObject workObj;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private MachineGeneratedContentService mgContentService;
    @Mock
    private BinaryObject altTextBinary, fullDescBinary, transcriptBinary;

    private PID filePid;
    private DerivativeService derivativeService;
    private SetContentDescriptionMetadataFilter filter;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        derivativeService = new DerivativeService();
        derivativeService.setDerivativeDir(tempDir.toFile().getAbsolutePath());

        filePid = PIDs.get(UUID.randomUUID().toString());

        when(dip.getDocument()).thenReturn(idb);
        when(fileObj.getPid()).thenReturn(filePid);

        filter = new SetContentDescriptionMetadataFilter();
        filter.setRepositoryObjectLoader(repositoryObjectLoader);
        filter.setMgContentService(mgContentService);
    }

    @AfterEach
    public void teardown() throws Exception {
        closeable.close();
    }

    @Test
    public void filter_nonFileObject_skipsProcessing() throws Exception {
        when(dip.getContentObject()).thenReturn(workObj);

        filter.filter(dip);

        verify(mgContentService, never()).loadMachineGeneratedDescription(any());
        verify(idb, never()).setMgDescription(any());
        verify(idb, never()).setAltText(any());
        verify(idb, never()).setFullDescription(any());
        verify(idb, never()).setTranscript(any());
        verify(idb, never()).setMgRiskScore(any());
        verify(idb, never()).setMgContentTags(any());
    }

    @Test
    public void filter_withAllFields_setsAllFieldsOnDocument() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);

        String defaultJson = Files.readString(Path.of(DEFAULT_JSON_PATH));
        JsonNode defaultNode = MachineGeneratedContentService.MAPPER.readTree(defaultJson);

        when(mgContentService.loadMachineGeneratedDescription(filePid)).thenReturn(defaultJson);
        when(mgContentService.deserializeMachineGeneratedDescription(defaultJson)).thenReturn(defaultNode);
        when(mgContentService.extractAltText(defaultNode)).thenReturn(ALT_TEXT_FROM_MG);
        when(mgContentService.extractFullDescription(defaultNode)).thenReturn(FULL_DESC_FROM_MG);
        when(mgContentService.extractTranscript(defaultNode)).thenReturn(TRANSCRIPT_FROM_MG);
        when(mgContentService.extractRiskScore(defaultNode)).thenReturn(10);
        when(mgContentService.extractContentTags(defaultNode)).thenReturn(List.of("tag1", "tag2"));
        // No alt text in fedora — fall back to machine generated
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getAltTextPid(filePid)))
                .thenThrow(new NotFoundException("No alt text"));
        // No full description in fedora — fall back to machine generated
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getFullDescriptionPid(filePid)))
                .thenThrow(new NotFoundException("No full description"));
        // No transcript in fedora — fall back to machine generated
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getTranscriptPid(filePid)))
                .thenThrow(new NotFoundException("No transcript"));

        filter.filter(dip);

        verify(idb).setMgDescription(defaultJson);
        verify(idb).setAltText(ALT_TEXT_FROM_MG);
        verify(idb).setFullDescription(FULL_DESC_FROM_MG);
        verify(idb).setTranscript(TRANSCRIPT_FROM_MG);
        verify(idb).setMgRiskScore(10);
        verify(idb).setMgContentTags(List.of("tag1", "tag2"));
    }

    @Test
    public void filter_noMachineGeneratedDescription_setsNullFields() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);

        String defaultJson = Files.readString(Path.of(DEFAULT_JSON_PATH));
        JsonNode defaultNode = MachineGeneratedContentService.MAPPER.readTree(defaultJson);

        when(mgContentService.loadMachineGeneratedDescription(filePid))
                .thenThrow(new NoSuchFileException("Not found"));
        when(mgContentService.deserializeMachineGeneratedDescription(isNull()))
                .thenThrow(new NullPointerException("Null pointer in deserialization"));
        when(mgContentService.extractAltText(defaultNode)).thenReturn(null);
        when(mgContentService.extractFullDescription(defaultNode)).thenReturn(null);
        when(mgContentService.extractTranscript(defaultNode)).thenReturn(null);
        when(mgContentService.extractRiskScore(defaultNode)).thenReturn(null);
        when(mgContentService.extractContentTags(defaultNode)).thenReturn(null);
        // No alt text in fedora
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getAltTextPid(filePid)))
                .thenThrow(new NotFoundException("No alt text"));
        // No full description in fedora — fall back to machine generated
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getFullDescriptionPid(filePid)))
                .thenThrow(new NotFoundException("No full description"));
        // No transcript in fedora — fall back to machine generated
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getTranscriptPid(filePid)))
                .thenThrow(new NotFoundException("No transcript"));

        filter.filter(dip);

        verify(idb).setMgDescription(null);
        verify(idb).setAltText(null);
        verify(idb).setFullDescription(null);
        verify(idb).setTranscript(null);
        verify(idb).setMgRiskScore(0);
        verify(idb).setMgContentTags(argThat(List::isEmpty));
    }

    @Test
    public void filter_machineGeneratedDescriptionThrowsIOException() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);

        String defaultJson = Files.readString(Path.of(DEFAULT_JSON_PATH));
        JsonNode defaultNode = MachineGeneratedContentService.MAPPER.readTree(defaultJson);

        when(mgContentService.loadMachineGeneratedDescription(filePid))
                .thenThrow(new IOException("Bad IO"));
        when(mgContentService.extractAltText(defaultNode)).thenReturn(null);
        when(mgContentService.extractFullDescription(defaultNode)).thenReturn(null);
        when(mgContentService.extractTranscript(defaultNode)).thenReturn(null);
        when(mgContentService.extractRiskScore(defaultNode)).thenReturn(null);
        when(mgContentService.extractContentTags(defaultNode)).thenReturn(null);
        // No alt text in fedora
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getAltTextPid(filePid)))
                .thenThrow(new NotFoundException("No alt text"));
        // No full description in fedora — fall back to machine generated
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getFullDescriptionPid(filePid)))
                .thenThrow(new NotFoundException("No full description"));
        // No transcript in fedora — fall back to machine generated
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getTranscriptPid(filePid)))
                .thenThrow(new NotFoundException("No transcript"));

        assertThrows(IndexingException.class, () -> filter.filter(dip));
    }

    @Test
    public void filter_repoAltTextFullDescTakesPrecedenceOverMgAltText() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);

        String defaultJson = Files.readString(Path.of(DEFAULT_JSON_PATH));
        JsonNode defaultNode = MachineGeneratedContentService.MAPPER.readTree(defaultJson);

        when(mgContentService.loadMachineGeneratedDescription(filePid)).thenReturn(defaultJson);
        when(mgContentService.deserializeMachineGeneratedDescription(defaultJson)).thenReturn(defaultNode);
        when(mgContentService.extractAltText(defaultNode)).thenReturn(ALT_TEXT_FROM_MG);
        when(mgContentService.extractFullDescription(defaultNode)).thenReturn(FULL_DESC_FROM_MG);
        when(mgContentService.extractTranscript(defaultNode)).thenReturn(null);
        when(mgContentService.extractRiskScore(defaultNode)).thenReturn(null);
        when(mgContentService.extractContentTags(defaultNode)).thenReturn(null);

        // Fedora has alt text stored — it should win
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getAltTextPid(filePid)))
                .thenReturn(altTextBinary);
        when(altTextBinary.getBinaryStream())
                .thenReturn(new ByteArrayInputStream(ALT_TEXT_FROM_REPO.getBytes(StandardCharsets.UTF_8)));

        // Fedora has full description stored — it should win
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getFullDescriptionPid(filePid)))
                .thenReturn(fullDescBinary);
        when(fullDescBinary.getBinaryStream())
                .thenReturn(new ByteArrayInputStream(FULL_DESC_FROM_REPO.getBytes(StandardCharsets.UTF_8)));

        // Fedora has transcript stored — it should win
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getTranscriptPid(filePid)))
                .thenReturn(transcriptBinary);
        when(transcriptBinary.getBinaryStream())
                .thenReturn(new ByteArrayInputStream(TRANSCRIPT_FROM_REPO.getBytes(StandardCharsets.UTF_8)));

        filter.filter(dip);

        verify(idb).setAltText(ALT_TEXT_FROM_REPO);
        verify(idb).setFullDescription(FULL_DESC_FROM_REPO);
        verify(idb).setTranscript(TRANSCRIPT_FROM_REPO);
    }

    @Test
    public void filter_mgTextUsedWhenNoRepoText() throws Exception {
        when(dip.getContentObject()).thenReturn(fileObj);

        String defaultJson = Files.readString(Path.of(DEFAULT_JSON_PATH));
        JsonNode defaultNode = MachineGeneratedContentService.MAPPER.readTree(defaultJson);

        when(mgContentService.loadMachineGeneratedDescription(filePid)).thenReturn(defaultJson);
        when(mgContentService.deserializeMachineGeneratedDescription(defaultJson)).thenReturn(defaultNode);
        when(mgContentService.extractAltText(defaultNode)).thenReturn(ALT_TEXT_FROM_MG);
        when(mgContentService.extractFullDescription(defaultNode)).thenReturn(FULL_DESC_FROM_MG);
        when(mgContentService.extractTranscript(defaultNode)).thenReturn(TRANSCRIPT_FROM_MG);
        when(mgContentService.extractRiskScore(defaultNode)).thenReturn(null);
        when(mgContentService.extractContentTags(defaultNode)).thenReturn(null);
        // No alt text in fedora
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getAltTextPid(filePid)))
                .thenThrow(new NotFoundException("No alt text"));
        // No full description in fedora
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getFullDescriptionPid(filePid)))
                .thenThrow(new NotFoundException("No full description"));
        // No transcript in fedora — fall back to machine generated
        when(repositoryObjectLoader.getBinaryObject(DatastreamPids.getTranscriptPid(filePid)))
                .thenThrow(new NotFoundException("No transcript"));

        filter.filter(dip);

        verify(idb).setAltText(ALT_TEXT_FROM_MG);
        verify(idb).setFullDescription(FULL_DESC_FROM_MG);
        verify(idb).setTranscript(TRANSCRIPT_FROM_MG);
    }
}
