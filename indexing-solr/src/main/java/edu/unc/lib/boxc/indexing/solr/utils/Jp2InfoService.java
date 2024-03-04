package edu.unc.lib.boxc.indexing.solr.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * @author bbpennel
 */
public class Jp2InfoService {
    private static final Logger log = LoggerFactory.getLogger(Jp2InfoService.class);

    private String kduCommand = "kdu_jp2info";

    private long totalExecTime = 0;
    private int execCount = 0;

    public Jp2Info getDimensions(Path path) {
        var start = System.nanoTime();
        JPEG2000KakaduImageReader kduReader = null;
        try {
            kduReader = new JPEG2000KakaduImageReader();
            kduReader.setSource(path);
            var height = kduReader.getHeight();
            var width = kduReader.getWidth();
            var ellapsed = System.nanoTime() - start;
            totalExecTime += ellapsed;
            execCount++;
            log.info("Got kdu dimensions for {} in {}ms", kduCommand, path.getFileName(), ellapsed / 1e6);
            log.info("Metrics: total time {}ms, total calls {}, average time {}ms",
                    totalExecTime / 1e6, execCount, (totalExecTime / execCount / 1e6));
            return new Jp2Info(width, height);
        } catch (IOException e) {
            log.warn("Failed to execute {} for {}: {}", kduCommand, path, e.getMessage());
//        } catch (InterruptedException e) {
//            log.warn("Interrupted {} for {}", kduCommand, path);
//            Thread.currentThread().interrupt();
//            throw new RuntimeException(e);
        } finally {
            if (kduReader != null) {
                kduReader.close();
            }
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

    public static class Jp2Info {
        private int width;
        private int height;

        public Jp2Info() {
        }

        public Jp2Info(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public String getExtent() {
            if (width == 0 || height == 0) {
                return "";
            }
            return height + "x" + width;
        }
    }
}
