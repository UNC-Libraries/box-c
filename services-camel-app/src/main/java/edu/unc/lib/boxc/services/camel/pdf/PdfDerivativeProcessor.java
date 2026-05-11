package edu.unc.lib.boxc.services.camel.pdf;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.pdf.PdfDerivativeService;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequest;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequestSerializationHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Processor which validates and prepares PDF objects for producing derivatives
 * @author krwong
 */
public class PdfDerivativeProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(PdfDerivativeProcessor.class);

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private PdfDerivativeService pdfDerivativeService;
    private String derivativeBasePath;

    private static final Pattern MIMETYPE_PATTERN = Pattern.compile("^(image.*$|application.*?(photoshop|psd|pdf)$)");

    private static final Pattern DISALLOWED_PATTERN =
            Pattern.compile(".*(vnd\\.fpx|x-icon|x-raw-panasonic|vnd\\.microsoft\\.icon).*");

    public PdfDerivativeProcessor(String derivativeBasePath) {
        this.derivativeBasePath = derivativeBasePath;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        var request = deserializeRequest(exchange);
        var agent = request.getAgent();
        var pid = PIDs.get(request.getWorkPid());
        var mimetype = request.getMimetype();
        Path derivativeFinalPath = setDerivativeFinalPath(request.getWorkPid());

        aclService.assertHasAccess("User does not have permission to run enhancements",
                pid, agent.getPrincipals(), Permission.runEnhancements);

        assertValid(pid, mimetype);

        try {
            Path derivativeTmpPath = pdfDerivativeService.generatePdfDerivative(request);
            moveFile(derivativeTmpPath, derivativeFinalPath);
        } catch (Exception e) {
            log.error("Failed to generate pdf derivative for {}: {}", pid, "");
            throw e;
        }
    }

    private PdfRequest deserializeRequest(Exchange exchange) {
        Message in = exchange.getIn();
        try {
            return PdfRequestSerializationHelper.toRequest(in.getBody(String.class));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize PDF derivative request", e);
        }
    }

    /**
     * Returns message if the work file is not eligible for having pdf derivatives generated from it
     * @param pid work pid
     * @param mimetype work mimetype
     * @return
     */
    private String validate(PID pid, String mimetype) {
        if (StringUtils.isBlank(mimetype)) {
            return "No mimetype provided for object " + pid;
        }

        if (!MIMETYPE_PATTERN.matcher(mimetype).matches()) {
            return "File type " + mimetype + " on object " + pid + " is not applicable for pdf derivatives";
        }

        if (DISALLOWED_PATTERN.matcher(mimetype).matches()) {
            return "File type " + mimetype + " on object " + pid + " is disallowed for pdf derivatives";
        }

        try {
            repositoryObjectLoader.getWorkObject(pid);
        } catch (ObjectTypeMismatchException e) {
            return "Object is not a WorkObject";
        }

        return null;
    }

    /**
     *  Throws an IllegalArgumentException if validate method returns any message
     * @param pid work pid
     * @param mimetype work mimetype
     */
    private void assertValid(PID pid, String mimetype) {
        var message = validate(pid, mimetype);
        if (!StringUtils.isBlank(message)) {
            throw new IllegalArgumentException(message);
        }
    }

    private Path setDerivativeFinalPath(String binaryId) {
        String derivativePath = idToPath(binaryId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        return Paths.get(derivativeBasePath,  derivativePath, binaryId + ".pdf");
    }

    private void moveFile(Path derivativeTmpPath, Path derivativeFinalPath)
            throws IOException {
        Files.createDirectories(derivativeFinalPath.getParent());

        log.debug("Moving derivative file from source {} to destination {}",
                derivativeTmpPath, derivativeFinalPath);

        Files.move(derivativeTmpPath, derivativeFinalPath, REPLACE_EXISTING);
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setPdfDerivativeService(PdfDerivativeService pdfDerivativeService) {
        this.pdfDerivativeService = pdfDerivativeService;
    }
}
