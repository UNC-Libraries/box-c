package edu.unc.lib.boxc.indexing.solr.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Jp2InfoService which uses Kakadu via shell calls in order to get info about jp2 images.
 *
 * @author bbpennel
 */
public class KduJp2InfoService implements Jp2InfoService {
    private static final Logger log = LoggerFactory.getLogger(KduJp2InfoService.class);

    private String kduCommand = "kdu_jp2info";

    private long totalExecTime = 0;
    private int execCount = 0;

    @Override
    public Jp2Info getDimensions(Path path) {
        var start = System.nanoTime();
        try {
            var command = Arrays.asList(kduCommand, "-i", path.toString());
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes());
            if (process.waitFor() == 0) {
                var width = extractKduDimension("<width>", output);
                var height = extractKduDimension("<height>", output);
                var elapsed = System.nanoTime() - start;
                totalExecTime += elapsed;
                execCount++;
                log.info("Performed {} for {} in {}ms", kduCommand, path.getFileName(), (elapsed / 1e6));
                log.info("Metrics: total time {}ms, total calls {}, average time {}ms",
                        totalExecTime / 1e6, execCount, (totalExecTime / execCount / 1e6));
                return new Jp2Info(width, height);
            }
            log.warn("Calling {} for {} exited with status code {}", kduCommand, path, process.waitFor());
            log.debug("Output from command: {}", output);
        } catch (IOException e) {
            log.warn("Failed to execute {} for {}: {}", kduCommand, path, e.getMessage());
        } catch (InterruptedException e) {
            log.warn("Interrupted {} for {}", kduCommand, path);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return new Jp2Info();
    }

    private int extractKduDimension(String openingTag, String kduOutput) {
        int startIndex = kduOutput.indexOf(openingTag);
        if (startIndex == -1) {
            return 0;
        }
        startIndex += openingTag.length();
        int endIndex = kduOutput.indexOf("</", startIndex);

        return Integer.parseInt(kduOutput.substring(startIndex, endIndex).trim());
    }

    public void setKduCommand(String kduCommand) {
        this.kduCommand = kduCommand;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

}
