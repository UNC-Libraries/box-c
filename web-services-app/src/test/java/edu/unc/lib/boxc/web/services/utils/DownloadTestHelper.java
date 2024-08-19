package edu.unc.lib.boxc.web.services.utils;

import org.apache.commons.io.FileUtils;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class DownloadTestHelper {
    public static void assertCorrectImageReturned(byte[] actualContent) throws IOException {
        byte[] imageContent = FileUtils.readFileToByteArray(new File("src/test/resources/__files/bunny.jpg"));

        assertArrayEquals(imageContent, actualContent);
    }


}
