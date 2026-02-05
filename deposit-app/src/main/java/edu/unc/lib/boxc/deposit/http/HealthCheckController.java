package edu.unc.lib.boxc.deposit.http;

import com.apicatalog.jsonld.StringUtils;
import edu.unc.lib.boxc.common.errors.CommandException;
import edu.unc.lib.boxc.common.util.CLIUtil;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Controller for performing health checks on the deposit app
 *
 * @author snluong
 */
public class HealthCheckController {
    private static final Logger log = LoggerFactory.getLogger(HealthCheckController.class);
    private static final long FITS_CLI_TIMEOUT_SECONDS = 60 * 5;
    private String fitsHomePath;
    private Path fitsCommandPath;

    /**
     * API endpoint which checks if the deposit app is reachable.
     * @return response code of 503 if checks are not successful, 200 if it is
     */
    @RequestMapping(value = "/health/depositAppUp", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<Object> isDepositAppUpCheck() {
        try {
            if (!isFitsAvailable()) {
                return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
            }
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private boolean isFitsAvailable() throws IOException {
        var tempFilePath = createTempFile();
        fitsCommandPath = Paths.get(fitsHomePath, "fits.sh");
        String[] command = new String[] { fitsCommandPath.toString(), "-i", tempFilePath.toString()};

        try {
            var joinedCommand = String.join(" ", command);
            log.debug("Health check FITS CLI command: {}", joinedCommand);
            var outcome = CLIUtil.executeCommand(List.of(command), FITS_CLI_TIMEOUT_SECONDS);
            var stderr = outcome.get(1);
            if (!StringUtils.isBlank(stderr)) {
                return false;
            }
        } catch (CommandException e) {
            return false;
        } finally {
            boolean deleted = Files.deleteIfExists(tempFilePath);
            if (deleted) {
                log.debug("Cleaned up health check temp file {}", tempFilePath);
            }
        }
        return true;
    }

    private Path createTempFile() throws IOException {
        var tempFile = File.createTempFile("file", ".txt");
        FileWriter writer = new FileWriter(tempFile);
        writer.write("boxc is best");
        writer.close();
        return tempFile.toPath();
    }

    public void setFitsHomePath(String fitsHomePath) {
        this.fitsHomePath = fitsHomePath;
    }
}
