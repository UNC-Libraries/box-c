package edu.unc.lib.boxc.web.services.processing;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author snluong
 */
public class SingleUseKeyServiceTest {
    @TempDir
    public Path tmpFolder;
    private String csvPath;

    private static final String UUID_1 = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String UUID_2 = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String UUID_3 = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";

    @BeforeEach
    public void setup() throws IOException {
        var csvDirectory = tmpFolder.resolve("singleUseKey");
        csvPath = File.createTempFile("accessKeys", ".csv", csvDirectory.toFile()).toString();
    }

    @Test
    public void testInvalidate() {

    }

    private void assertDoesNotContainEntry(List<CSVRecord> csvRecords, String key) {
        for (CSVRecord record : csvRecords) {
            if (key.equals(record.get(SingleUseKeyService.ACCESS_KEY))) {
                fail("Entry found for uuid " + key);
                return;
            }
        }
    }

    private void generateCsv(String key) {
    }
}
