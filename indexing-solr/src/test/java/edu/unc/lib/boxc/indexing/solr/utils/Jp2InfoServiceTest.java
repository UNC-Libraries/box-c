package edu.unc.lib.boxc.indexing.solr.utils;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author bbpennel
 */
public class Jp2InfoServiceTest {
    private Jp2InfoService service;

    @BeforeEach
    public void setup() {
        service = new Jp2InfoService();
    }

    @Test
    public void getDimensionsTest() throws Exception {
        String data = "<JP2_family_file>\n" +
                " <ftyp name=\"file-type box\" header=\"8\" body=\"12\" pos=\"12\">\n" +
                "   <brand> \"jp2_\" 0x6A703220 </brand>\n" +
                "   <minor_version> 0 </minor_version>\n" +
                "   <compatible_brand> \"jp2_\" 0x6A703220 </compatible_brand>\n" +
                " </ftyp>\n" +
                " <jp2h name=\"JP2-header box\" header=\"8\" body=\"37\" pos=\"32\">\n" +
                "   <ihdr name=\"image-header box\" header=\"8\" body=\"14\" pos=\"40\"></ihdr>\n" +
                "   <colr name=\"colour box\" header=\"8\" body=\"7\" pos=\"62\"></colr>\n" +
                " </jp2h>\n" +
                " <jp2c name=\"contiguous-codestream box\" header=\"8\" body=\"rubber\" pos=\"77\">\n" +
                "   <codestream>\n" +
                "     <width> 3024 </width>\n" +
                "     <height> 3414 </height>\n" +
                "     <components> 3 </components>\n" +
                "     <tiles> 42 </tiles>\n" +
                "   </codestream>\n" +
                "</JP2_family_file>";
        var kduPath = makeKduScript("echo '" + data + "'");
        service.setKduCommand(kduPath);

        Path imagePath = Path.of("path/to/image.jp2");
        var info = service.getDimensions(imagePath);
        assertEquals("3414x3024", info.getExtent());
    }

    @Test
    public void getDimensionsFailureTest() throws Exception {
        var kduPath = makeKduScript("exit 1");
        service.setKduCommand(kduPath);

        Path imagePath = Path.of("path/to/image.jp2");
        var info = service.getDimensions(imagePath);
        assertEquals("", info.getExtent());
    }

    public static String makeKduScript(String action) throws Exception {
        String scriptContent = "#!/usr/bin/env bash"
                + "\n" + action;
        File kduScript = File.createTempFile("fake_kdu", ".sh");

        FileUtils.write(kduScript, scriptContent, "UTF-8");

        kduScript.deleteOnExit();

        Set<PosixFilePermission> ownerExecutable = PosixFilePermissions.fromString("rwx------");
        Files.setPosixFilePermissions(kduScript.toPath(), ownerExecutable);

        return kduScript.getAbsolutePath();
    }
}
