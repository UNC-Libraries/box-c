package edu.unc.lib.boxc.services.camel.longleaf;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Helpers for longleaf tests
 *
 * @author bbpennel
 */
public class LongleafTestHelpers {

    private LongleafTestHelpers() {
    }

    /**
     * Generates a longleaf test script which records all commands called through it
     *
     * @param outputPath
     * @return
     * @throws Exception
     */
    public static String getLongleafScript(String outputPath) throws Exception {
        String scriptContent = "#!/usr/bin/env bash"
                + "\necho $@ >> " + outputPath
                + "\necho \"$(</dev/stdin)\" >> " + outputPath;
        File longleafScript = File.createTempFile("longleaf", ".sh");

        FileUtils.write(longleafScript, scriptContent, "UTF-8");

        longleafScript.deleteOnExit();

        Set<PosixFilePermission> ownerExecutable = PosixFilePermissions.fromString("rwx------");
        Files.setPosixFilePermissions(longleafScript.toPath(), ownerExecutable);

        return longleafScript.getAbsolutePath();
    }

    public static List<String> readOutput(String outputPath) throws IOException {
        String outputText = FileUtils.readFileToString(new File(outputPath), UTF_8);
        if (StringUtils.isEmpty(outputText)) {
            return Collections.emptyList();
        }
        return Arrays.asList(outputText.split("\n"));
    }
}
