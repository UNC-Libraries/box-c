package edu.unc.lib.boxc.deposit.http;

import edu.unc.lib.boxc.common.errors.CommandException;
import edu.unc.lib.boxc.common.util.CLIUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
@Controller
public class HealthCheckController {
    private static final Logger log = LoggerFactory.getLogger(HealthCheckController.class);
    public static final long FITS_CLI_TIMEOUT_SECONDS = 60 * 5;
    @Autowired
    private String fitsHomePath;

    /**
     * API endpoint which checks if the deposit app is reachable.
     * @return response code of 503 if checks are not successful, 200 if it is
     */
    @RequestMapping(value = "/health/depositAppUp", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<Object> isDepositAppUpCheck() {
        var healthStatus = new HealthStatus();
        try {
            if (!isFitsAvailable()) {
                healthStatus.setStatus(HealthStatus.DOWN);
                healthStatus.setMessage(HealthStatus.FITS_UNAVAILABLE);
                return new ResponseEntity<>(healthStatus, HttpStatus.SERVICE_UNAVAILABLE);
            }
        } catch (IOException e) {
            healthStatus.setStatus(HealthStatus.DOWN);
            healthStatus.setMessage(HealthStatus.SERVER_PROBLEM);
            return new ResponseEntity<>(healthStatus, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        healthStatus.setStatus(HealthStatus.UP);
        healthStatus.setMessage(HealthStatus.OK);
        return new ResponseEntity<>(healthStatus, HttpStatus.OK);
    }

    private boolean isFitsAvailable() throws IOException {
        var tempFilePath = createTempFile();
        var fitsCommandPath = Paths.get(fitsHomePath, "fits.sh");
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

    public static class HealthStatus {
        public static final String UP = "UP";
        public static final String DOWN = "DOWN";
        public static final String OK = "OK";
        public static final String FITS_UNAVAILABLE = "FITS service unavailable";
        public static final String SERVER_PROBLEM = "Something went wrong";
        private String status;
        private String message;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
