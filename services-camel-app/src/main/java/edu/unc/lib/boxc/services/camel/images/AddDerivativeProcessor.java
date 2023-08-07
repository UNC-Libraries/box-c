package edu.unc.lib.boxc.services.camel.images;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.exec.ExecResult;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

/**
 * Adds a derivative file to an existing file object
 *
 * @author bbpennel
 * @author harring
 * @author lfarrell
 *
 */
public class AddDerivativeProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AddDerivativeProcessor.class);

    private final String fileExtension;
    private final String derivativeBasePath;

    private final static Pattern ERROR_PATTERN = Pattern.compile("^(.+ @ error/.+)$", Pattern.MULTILINE);
    private final static String IGNORE_ERROR = "; tag ignored.";

    public AddDerivativeProcessor(String fileExtension, String derivativeBasePath) {
        this.fileExtension = fileExtension;
        this.derivativeBasePath = derivativeBasePath;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();

        String binaryUri = (String) in.getHeader(FCREPO_URI);
        String binaryId = PIDs.get(binaryUri).getId();
        Path derivativeFinalPath = setDerivativeFinalPath(binaryId);

        final ExecResult result = (ExecResult) in.getBody();
        String stdout = outputToString(result.getStdout());
        String stderr = outputToString(result.getStderr());
        try {
            // Prevent further processing if the execution failed
            if (result.getExitValue() != 0) {
                assertNoFatalErrors(stderr, stdout, binaryId);
                log.debug("Result returned error code {} for {} but no errors were present in the output,"
                        + " derivative will be added: {}", result.getExitValue(), binaryId, stderr);
            }

            // Read command result as path to derived file, and trim off trailing whitespace
            var derivativeTmpPath = Paths.get(stdout + "." + fileExtension);
            assertDerivativePathValid(derivativeTmpPath, binaryId);
            if (Files.notExists(derivativeTmpPath)) {
                // Command was successful, but no derivative exists, so we will assume this is the intended state
                if (result.getExitValue() == 0) {
                    log.info("No derivative was generated for {} after successful command.", binaryId);
                    return;
                } else {
                    throw new DerivativeGenerationException("No derivative was generated for " + binaryId
                            + " and command failed with response: " + stderr);
                }
            }

            moveFile(derivativeTmpPath, derivativeFinalPath);
            log.info("Added derivative for {} from {}", binaryUri, derivativeFinalPath);
        } catch (IOException e) {
            log.error("Failed to generate derivative to {} for {}: {}", derivativeBasePath, binaryId, stderr);
            throw e;
        }
    }

    private boolean assertNoFatalErrors(String stderr, String stdout, String binaryId) throws IOException {
        Matcher errorMatcher = ERROR_PATTERN.matcher(stderr);
        while (errorMatcher.find()) {
            String errorString = errorMatcher.group(1);
            if (errorString.contains(IGNORE_ERROR)) {
                log.debug("Ignoring error message for {}: {}", binaryId, errorString);
            } else {
                throw new DerivativeGenerationException("Failed to generate derivative for " + binaryId
                        + ": " + stdout + " " + stderr);
            }
        }
        return false;
    }

    private void assertDerivativePathValid(Path derivativeTmpPath, String binaryId) {
        if (!derivativeTmpPath.isAbsolute()) {
            throw new DerivativeGenerationException("Path returned by derivative command for " + binaryId
                    + " returned a relative path: " + derivativeTmpPath);
        }
    }

    private String outputToString(InputStream output) throws IOException {
        return (output != null) ? IOUtils.toString(output, UTF_8).trim() : "";
    }

    /**
     * Used to filter whether enhancements should be run
     * @param exchange Camel message exchange
     * @return
     */
    public boolean needsRun(Exchange exchange) {
        Message in = exchange.getIn();

        String binaryUri = (String) in.getHeader(FCREPO_URI);
        String binaryId = PIDs.get(binaryUri).getId();
        Path derivativeFinalPath = setDerivativeFinalPath(binaryId);
        if (Files.notExists(derivativeFinalPath)) {
            log.debug("Derivative run needed, no existing derivative for {} in {}", binaryId, derivativeBasePath);
            return true;
        }

        String force = (String) in.getHeader("force");
        if (Boolean.parseBoolean(force)) {
            log.debug("Force flag was provided, forcing run of already existing derivative for {} in {}",
                    binaryId, derivativeBasePath);
            return true;
        } else {
            log.debug("Derivative already exists for {} in {}, run not needed", binaryId, derivativeBasePath);
            return false;
        }
    }

    /**
     * Deletes a temp file listed in the CdrTempPath header if it is present
     *
     * @param exchange
     * @throws Exception
     */
    public void cleanupTempFile(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        String tempValue = (String) in.getHeader(CdrFcrepoHeaders.CdrTempPath);
        tempValue += "." + fileExtension;
        Path tempPath = Paths.get(tempValue);

        boolean deleted = Files.deleteIfExists(tempPath);
        if (deleted) {
            log.debug("Cleaned up leftover temp file {}", tempPath);
        }
    }

    private Path setDerivativeFinalPath(String binaryId) {
        String derivativePath = idToPath(binaryId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        return Paths.get(derivativeBasePath,  derivativePath, binaryId + "." + fileExtension);
    }

    private void moveFile(Path derivativeTmpPath, Path derivativeFinalPath)
            throws IOException {
        Files.createDirectories(derivativeFinalPath.getParent());

        log.debug("Moving derivative file from source {} to destination {}",
                    derivativeTmpPath, derivativeFinalPath);

        Files.move(derivativeTmpPath, derivativeFinalPath, REPLACE_EXISTING);
    }

    public static class DerivativeGenerationException extends RuntimeException {
        public DerivativeGenerationException(String message) {
            super(message);
        }
    }
}
