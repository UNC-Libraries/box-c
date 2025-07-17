package edu.unc.lib.boxc.web.services.utils;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.commons.io.FileUtils;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class DownloadTestHelper {
    public static void assertCorrectImageReturned(byte[] actualContent) throws IOException {
        byte[] imageContent = FileUtils.readFileToByteArray(new File("src/test/resources/__files/bunny.jpg"));

        assertArrayEquals(imageContent, actualContent);
    }

    public static PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
