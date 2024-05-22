package edu.unc.lib.boxc.integration.factories;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.rdf.IanaRelation;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import org.apache.commons.io.FileUtils;
import org.apache.jena.vocabulary.DCTerms;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.xml.NamespaceConstants.FITS_URI;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

/**
 * Factory for creating test FileObjects
 * @author snluong
 */
public class FileFactory extends ContentObjectFactory {
    public static final String FILE_FORMAT_OPTION = "fileFormat";
    public static final String IMAGE_FORMAT = "image";
    public static final String TEXT_FORMAT = "text";
    public static final String PDF_FORMAT = "pdf";
    public static final String AUDIO_FORMAT = "audio";
    public static final String VIDEO_FORMAT = "video";
    private StorageLocationTestHelper storageLocationTestHelper;

    /*
    Creates a basic File with a FileFormat
    Needs WorkFactory createFileInWork to be a legitimate File with a Work as a parent
     */
    public FileObject createFile(Map<String, String> options) throws Exception {
        var accessModel = getAccessModel(options);
        var file = repositoryObjectFactory.createFileObject(accessModel);
        addFileFormat(file, options);

        return file;
    }

    private void addFileFormat(FileObject file, Map<String, String> options) throws Exception {
        var fileFormat = options.get(FILE_FORMAT_OPTION);

        switch (fileFormat) {
        case IMAGE_FORMAT:
            createOriginalFile(file, "image/jpeg", "data", ".jpg");
            addThumbnail(file);

            var jp2Path = derivativeService.getDerivativePath(file.getPid(), DatastreamType.JP2_ACCESS_COPY);
            FileUtils.write(jp2Path.toFile(), "image", "UTF-8");
            break;
        case TEXT_FORMAT:
            var data = "we have a lot of text to get through. more than usual";
            createOriginalFile(file, "text/plain", data, ".txt");
            // full text derivatives
            var textPath = derivativeService.getDerivativePath(file.getPid(), DatastreamType.FULLTEXT_EXTRACTION);
            FileUtils.write(textPath.toFile(), data, "UTF-8");
            break;
        case PDF_FORMAT:
            var pdfData = "this is a very legitimate PDF";
            createOriginalFile(file, "application/pdf", pdfData,".pdf");
            // full text derivatives
            var pdfTextPath = derivativeService.getDerivativePath(file.getPid(), DatastreamType.FULLTEXT_EXTRACTION);
            FileUtils.write(pdfTextPath.toFile(), pdfData, "UTF-8");
            break;
        case AUDIO_FORMAT:
            createOriginalFile(file, "audio/mp3", "This is an mp3", ".mp3");
            break;
        case VIDEO_FORMAT:
            createOriginalFile(file, "video/mp4", "This is a video", ".mp4");
            break;
        default:
            throw new IllegalArgumentException("File format: " + fileFormat +
                    " isn't recognized in FileFactory creation.");
        }

        var fitsPid = DatastreamPids.getTechnicalMetadataPid(file.getPid());
        var fitsUri = Objects.requireNonNull(
                this.getClass().getResource("/datastream/techmd_image.xml")).toURI();
        var contentUri = storageLocationTestHelper.makeTestStorageUri(fitsPid);
        Files.copy(Path.of(fitsUri), Path.of(contentUri));
        // add FITS file. Same one for all formats at the moment
        file.addBinary(fitsPid, contentUri, TECHNICAL_METADATA.getDefaultFilename(), TECHNICAL_METADATA.getMimetype(),
                null, null, IanaRelation.derivedfrom, DCTerms.conformsTo, createResource(FITS_URI));
    }

    private void createOriginalFile(FileObject file, String mimetype, String data, String suffix) throws IOException {
        var contentUri = storageLocationTestHelper.makeTestStorageUri(DatastreamPids.getOriginalFilePid(file.getPid()));
        FileUtils.write(new File(contentUri), data, "UTF-8");
        file.addOriginalFile(contentUri, "test_file" + suffix, mimetype, null, null);
    }

    public void setStorageLocationTestHelper(StorageLocationTestHelper storageLocationTestHelper) {
        this.storageLocationTestHelper = storageLocationTestHelper;
    }
}
