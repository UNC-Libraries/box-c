package edu.unc.lib.boxc.web.services.processing;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static edu.unc.lib.boxc.web.services.processing.SingleUseKeyService.CSV_HEADERS;
import static edu.unc.lib.boxc.web.services.processing.SingleUseKeyService.DAY_MILLISECONDS;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.createCsvPrinter;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.parseCsv;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author snluong
 */
public class SingleUseKeyServiceTest {
    @TempDir
    public Path tmpFolder;
    private Path csvPath;

    private static final String UUID_1 = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String UUID_2 = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String UUID_3 = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String UUID_TEST = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private String[] ids = {UUID_1, UUID_2, UUID_3};
    private SingleUseKeyService singleUseKeyService;

    @BeforeEach
    public void setup() throws IOException {
        csvPath = tmpFolder.resolve("singleUseKey");
        singleUseKeyService = new SingleUseKeyService();
        singleUseKeyService.setCsvPath(csvPath);
    }

    @Test
    public void testGenerate() throws IOException {
        generateDefaultCsv(null);
        var oldRecords = parseCsv(CSV_HEADERS, csvPath);
        assertDoesNotContainValue(oldRecords, SingleUseKeyService.ID, UUID_TEST);

        var key = singleUseKeyService.generate(UUID_TEST);
        var newRecords = parseCsv(CSV_HEADERS, csvPath);
        assertContainsAccessKeyPair(newRecords, UUID_TEST, key);
    }

    @Test
    public void testKeyIsValid() throws IOException {
        var key = SingleUseKeyService.getKey();
        generateDefaultCsv(key);
        var currentMilliseconds = System.currentTimeMillis();
        assertTrue(singleUseKeyService.keyIsValid(UUID_TEST, key, currentMilliseconds));
    }

    @Test
    public void testKeyIsNotValid() throws IOException {
        var key = SingleUseKeyService.getKey();
        generateDefaultCsv(null);
        var currentMilliseconds = System.currentTimeMillis();
        assertFalse(singleUseKeyService.keyIsValid(UUID_TEST, key, currentMilliseconds));
    }

    @Test
    public void testKeyIsNotValidWrongUUID() throws IOException {
        var key = SingleUseKeyService.getKey();
        generateDefaultCsv(key);
        var currentMilliseconds = System.currentTimeMillis();
        assertFalse(singleUseKeyService.keyIsValid(
                "8e0040b2-9951-48a3-9d65-780ae7106951", key, currentMilliseconds));
    }

    @Test
    public void testKeyIsNotValidCurrentTimeIsMoreThan24hLater() throws IOException {
        var key = SingleUseKeyService.getKey();
        generateDefaultCsv(key);
        var currentMilliseconds = System.currentTimeMillis() + (2 * DAY_MILLISECONDS);
        assertFalse(singleUseKeyService.keyIsValid(UUID_TEST, key, currentMilliseconds));
    }

    @Test
    public void testInvalidate() throws IOException {
        var key = SingleUseKeyService.getKey();
        generateDefaultCsv(key);
        singleUseKeyService.invalidate(key);

        var records = parseCsv(CSV_HEADERS, csvPath);
        assertDoesNotContainValue(records, SingleUseKeyService.ACCESS_KEY, key);
    }

    private void assertDoesNotContainValue(List<CSVRecord> csvRecords, String column, String value) {
        for (CSVRecord record : csvRecords) {
            if (value.equals(record.get(column))) {
                fail("Entry found!");
                return;
            }
        }
    }

    private void assertContainsAccessKeyPair(List<CSVRecord> csvRecords, String id, String key) {
        for (CSVRecord record : csvRecords) {
            if (id.equals(record.get(SingleUseKeyService.ID))) {
                assertEquals(key, record.get(SingleUseKeyService.ACCESS_KEY));
                return;
            }
        }
        fail("No access key pair found");
    }

    private void generateDefaultCsv(String key) throws IOException {
        try (var csvPrinter = createCsvPrinter(CSV_HEADERS, csvPath)) {
             for (String id : ids) {
                 csvPrinter.printRecord(id, SingleUseKeyService.getKey(), System.currentTimeMillis());
             }
             if (key != null) {
                 csvPrinter.printRecord(UUID_TEST, key, System.currentTimeMillis());
             }
        }
    }
}
