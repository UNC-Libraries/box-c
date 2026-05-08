package edu.unc.lib.boxc.operations.impl.pdf;

import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pdf4u.CLIMain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static edu.unc.lib.boxc.operations.impl.utils.ExternalDerivativesUtil.getDerivativePath;

/**
 * Service for generating a derivative PDF with OCR
 * @author krwong
 */
public class PdfDerivativeService {
    private static final Logger log = LoggerFactory.getLogger(PdfDerivativeService.class);
    private RepositoryObjectLoader repositoryObjectLoader;
    private String derivativeBasePath;

    public Path generatePdfDerivative(PdfRequest request) throws IOException {
        var pid = request.getWorkPid();
        String inputFiles = getInputFiles(request);
        String transcriptFiles = getTranscriptFiles(request);
        Path tempPath = Path.of("temp");
        //TODO: get text type from the alt text review
        String textType = "HANDWRITTEN-PRINT";

        try {
            // check that object is a work object
            repositoryObjectLoader.getWorkObject(pid);

            String[] command = new String[]{"pdf4u", "add_ocr", "-i", inputFiles, "-o", tempPath.toString(),
                    "-t", transcriptFiles, "-tt", textType};
            log.debug("Run pdf4u command {} for work {}", command, pid);
            CLIMain.runCommand(command);

            return tempPath;
        } catch (ObjectTypeMismatchException e) {
            log.debug("Object {} is not a work object", request.getWorkPid(), e);
            throw new IllegalArgumentException("Object " + request.getWorkPid() + " is not a work object");
        } catch (Exception e) {
            log.error("Failed to generate pdf derivative to {} for {}", tempPath, pid);
            throw e;
        } finally {
            List<String> temporaryFiles = Arrays.asList(inputFiles, transcriptFiles);
            for (String tempFile : temporaryFiles) {
                Files.deleteIfExists(Path.of(tempFile));
            }
        }
    }

    /**
     * Get path for input file(s)
     * @param request PdfRequest
     * @return .txt path to input files
     */
    public String getInputFiles(PdfRequest request) {
        var pid = request.getWorkPid();

        var inputFilePath = "tempPath";
        try {
            // check that object is a work object


            return inputFilePath;
        } catch (Exception e) {
            log.debug("Object {} is not a work object", request.getWorkPid(), e);
            throw new IllegalArgumentException("Object " + request.getWorkPid() + " is not a work object");
        }
    }

    /**
     * Get path for transcript file(s)
     * @param request PdfRequest
     * @return .txt path to transcript files
     */
    public String getTranscriptFiles(PdfRequest request) {
        //TODO: get transcript files from the alt text review

        return "tempTranscriptPath";
    }

    /**
     * Get path for PDF with OCR derivative
     * @param id binary ID
     * @return path to derivative
     */
    public Path getPdfDerivativePath(String id) {
        return getDerivativePath(derivativeBasePath, id);
    }


    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }
}
