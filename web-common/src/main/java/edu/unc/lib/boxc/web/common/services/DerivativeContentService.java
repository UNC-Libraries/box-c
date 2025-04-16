package edu.unc.lib.boxc.web.common.services;

import static edu.unc.lib.boxc.auth.api.services.DatastreamPermissionUtil.getPermissionForDatastream;
import static edu.unc.lib.boxc.model.api.DatastreamType.getByIdentifier;
import static edu.unc.lib.boxc.model.fcrepo.services.DerivativeService.listDerivativeTypes;
import static edu.unc.lib.boxc.web.common.services.FedoraContentService.CONTENT_DISPOSITION;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService.Derivative;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Streams content for derivative files of repository objects.
 *
 * @author bbpennel
 *
 */
public class DerivativeContentService {
    private static final Logger log = LoggerFactory.getLogger(DerivativeContentService.class);

    private static final int BUFFER_SIZE = 8192;

    private DerivativeService derivativeService;

    private AccessControlService accessControlService;

    public DerivativeContentService() {
    }

    /**
     * Set content headers and stream the content of the specified derivative
     * from the object identified by pid.
     *
     * @param pid pid of object containing the derivative
     * @param dsName name of derivative being requested. Must be a derivative
     *            type, otherwise an IllegalArgumentException will be thrown.
     * @param principals principals of requesting client
     * @param asAttachment if true, then content-disposition header will specify
     *            as "attachment" instead of "inline"
     * @param rangeHeader the range header value from the request, or null
     * @param response response content and headers will be added to.
     * @throws IOException if unable to stream content to the response.
     * @throws ResourceNotFoundException if an invalid derivative type is
     *             requested.
     */
    public void streamData(PID pid, String dsName, AccessGroupSet principals, boolean asAttachment, String rangeHeader,
            HttpServletResponse response) throws IOException, ResourceNotFoundException {

        DatastreamType derivType = getType(dsName);

        accessControlService.assertHasAccess("Insufficient permissions to access derivative "
                + dsName + " for object " + pid,
                pid, principals, getPermissionForDatastream(derivType));

        Derivative deriv = getDerivative(pid, dsName, derivType);

        File derivFile = deriv.getFile();
        response.setContentType(derivType.getMimetype());
        String filename = derivFile.getName();
        if (asAttachment) {
            response.setHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        } else {
            response.setHeader(CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
        }
        if (rangeHeader == null) {
            streamEntireFile(derivFile, response);
        } else {
            streamRange(derivFile, rangeHeader, response);
        }
    }

    private void streamEntireFile(File file, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentLengthLong(file.length());

        OutputStream outStream = response.getOutputStream();
        IOUtils.copy(new FileInputStream(file), outStream, BUFFER_SIZE);
    }

    private void streamRange(File file, String rangeHeader, HttpServletResponse response) throws IOException {
        long fileLength = file.length();
        try {
            List<HttpRange> httpRanges = HttpRange.parseRanges(rangeHeader);
            // Currently only supporting single range requests
            if (httpRanges.size() > 1) {
                throw new IllegalArgumentException("Only single range requests are supported");
            }
            // If parsing produced no ranges, stream the entire file
            if (httpRanges.isEmpty()) {
                streamEntireFile(file, response);
                return;
            }

            // Get the single range
            HttpRange range = httpRanges.get(0);
            long start = range.getRangeStart(fileLength);
            long end = range.getRangeEnd(fileLength);
            long contentLength = end - start + 1;

            // Set appropriate headers for partial content
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            response.setContentLengthLong(contentLength);
            response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength);

            // Stream the requested range
            try (FileInputStream inputStream = new FileInputStream(file)) {
                // Skip to the start position
                var skipped = inputStream.skip(start);
                if (skipped != start) {
                    throw new IOException("Unable to skip to the requested range start: " + start);
                }
                // Copy only the requested range
                copyRangeBytesToResponse(inputStream, response, contentLength);
            }
        } catch (IllegalArgumentException e) {
            log.debug("Failed to parse range header: {}", rangeHeader, e);
            // HttpRange will throw IllegalArgumentException for invalid range header values
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setHeader("Content-Range", "bytes */" + fileLength);
        }
    }

    private void copyRangeBytesToResponse(FileInputStream inputStream,
            HttpServletResponse response, long contentLength) throws IOException {
        var outStream = response.getOutputStream();
        long bytesToCopy = contentLength;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        try {
            while (bytesToCopy > 0 && (bytesRead =
                    inputStream.read(buffer, 0, (int) Math.min(buffer.length, bytesToCopy))) != -1) {
                outStream.write(buffer, 0, bytesRead);
                bytesToCopy -= bytesRead;
            }
        } catch (IOException e) {
            // Silently ignore IO errors while streaming, such as the client closing the connection
            log.debug("IO error while streaming range: {}", e.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private DatastreamType getType(String dsName) {
        DatastreamType derivType = getByIdentifier(dsName);
        if (derivType == null || !listDerivativeTypes().contains(derivType)) {
            throw new IllegalArgumentException(dsName + " is not a valid derivative type.");
        }
        return derivType;
    }

    private Derivative getDerivative(PID pid, String dsName, DatastreamType datastreamType) {
        Derivative derivative = derivativeService.getDerivative(pid, datastreamType);
        if (derivative == null) {
            throw new ResourceNotFoundException("Derivative " + dsName + " does not exist for object " + pid);
        }
        return derivative;
    }

    /**
     * @param derivativeService the derivativeService to set
     */
    public void setDerivativeService(DerivativeService derivativeService) {
        this.derivativeService = derivativeService;
    }

    /**
     * @param accessControlService the accessControlService to set
     */
    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }
}
