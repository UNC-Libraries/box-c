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
package edu.unc.lib.boxc.integration.factories;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import org.apache.commons.io.FileUtils;
import edu.unc.lib.boxc.model.api.rdf.IanaRelation;
import org.apache.jena.vocabulary.DCTerms;

import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.xml.NamespaceConstants.FITS_URI;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

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

    private DerivativeService derivativeService;

    public FileObject createFile(Map<String, String> options) throws Exception {
        var file = repositoryObjectFactory.createFileObject(null);
        addFileFormat(file, options);
        prepareObject(file, options);

        return file;
    }

    private void addFileFormat(FileObject file, Map<String, String> options) throws Exception {
        var fileFormat = options.get(FILE_FORMAT_OPTION);

        switch (fileFormat) {
        case IMAGE_FORMAT:
            createOriginalFile(file, "image/jpeg", "data", ".jpg");

            var derivativePath = derivativeService.getDerivativePath(file.getPid(), DatastreamType.THUMBNAIL_LARGE);
            FileUtils.write(derivativePath.toFile(), "image", "UTF-8");

            var jp2Path = derivativeService.getDerivativePath(file.getPid(), DatastreamType.JP2_ACCESS_COPY);
            FileUtils.write(jp2Path.toFile(), "image", "UTF-8");

            var fitsPid = DatastreamPids.getTechnicalMetadataPid(file.getPid());
            var fitsUri = Objects.requireNonNull(
                    this.getClass().getResource("/datastream/techmd_image.xml")).toURI();
            // add FITS file
            file.addBinary(fitsPid, fitsUri, TECHNICAL_METADATA.getDefaultFilename(), TECHNICAL_METADATA.getMimetype(),
                    null, null, IanaRelation.derivedfrom, DCTerms.conformsTo, createResource(FITS_URI));
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
    }

    public void setDerivativeService(DerivativeService derivativeService) {
        this.derivativeService = derivativeService;
    }

    private void createOriginalFile(FileObject file, String mimetype, String data, String suffix) throws IOException {
        File contentFile = File.createTempFile("test_file", suffix);
        FileUtils.write(contentFile, data, "UTF-8");
        contentFile.deleteOnExit();
        var contentUri = contentFile.toPath().toUri();
        file.addOriginalFile(contentUri, "test_file" + suffix, mimetype, null, null);
    }
}
