package edu.unc.lib.boxc.web.services.processing;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static edu.unc.lib.boxc.web.services.processing.SingleUseKeyService.CSV_HEADERS;
import static edu.unc.lib.boxc.web.services.processing.SingleUseKeyService.DAY_MILLISECONDS;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.createCsvPrinter;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.parseCsv;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private final String[] ids = {UUID_1, UUID_2, UUID_3};
    private SingleUseKeyService singleUseKeyService;

    @BeforeEach
    public void setup() throws IOException {
        csvPath = tmpFolder.resolve("singleUseKey");
        singleUseKeyService = new SingleUseKeyService();
        singleUseKeyService.setCsvPath(csvPath);
    }

    @Test
    public void testGenerateNoCsvExists() throws IOException {
        var key = parseKey(singleUseKeyService.generate(UUID_TEST));
        var newRecords = parseCsv(CSV_HEADERS, csvPath);
        assertCsvContainsCorrectEntry(newRecords, UUID_TEST, key);
        assertEquals(1, newRecords.size());
    }

    @Test
    public void testGenerateMultipleCallsForDifferentIds() throws IOException {
        var key1 = parseKey(singleUseKeyService.generate(UUID_1));
        var key2 = parseKey(singleUseKeyService.generate(UUID_2));
        var key3 = parseKey(singleUseKeyService.generate(UUID_3));

        var newRecords = parseCsv(CSV_HEADERS, csvPath);
        assertCsvContainsCorrectEntry(newRecords, UUID_1, key1);
        assertCsvContainsCorrectEntry(newRecords, UUID_2, key2);
        assertCsvContainsCorrectEntry(newRecords, UUID_3, key3);
        assertEquals(3, newRecords.size());
    }

    @Test
    public void testGenerateMultipleCallsForSameId() throws IOException {
        var key1 = parseKey(singleUseKeyService.generate(UUID_1));
        var key2 = parseKey(singleUseKeyService.generate(UUID_1));
        var key3 = parseKey(singleUseKeyService.generate(UUID_1));

        var newRecords = parseCsv(CSV_HEADERS, csvPath);
        assertCsvContainsCorrectEntry(newRecords, UUID_1, key1);
        assertCsvContainsCorrectEntry(newRecords, UUID_1, key2);
        assertCsvContainsCorrectEntry(newRecords, UUID_1, key3);
        assertEquals(3, newRecords.size());

    }

    @Test
    public void testKeyIsValid() throws IOException {
        var key = SingleUseKeyService.getKey();
        var futureExpiration = System.currentTimeMillis() + DAY_MILLISECONDS;
        generateDefaultCsv(key, futureExpiration);
        assertTrue(singleUseKeyService.keyIsValid(key));
    }

    @Test
    public void testKeyIsNotValid() throws IOException {
        var key = SingleUseKeyService.getKey();
        var timestamp = System.currentTimeMillis();
        generateDefaultCsv(null, timestamp);
        assertFalse(singleUseKeyService.keyIsValid(key));
    }

    @Test
    public void testKeyIsNotValidCurrentTimeIsMoreThan24hLater() throws IOException {
        var key = SingleUseKeyService.getKey();
        var pastTimestamp = System.currentTimeMillis() - (2 * DAY_MILLISECONDS);
        generateDefaultCsv(key, pastTimestamp);
        assertFalse(singleUseKeyService.keyIsValid(key));
    }

    @Test
    public void testInvalidate() throws IOException {
        var key = SingleUseKeyService.getKey();
        var expirationTimestamp = System.currentTimeMillis() + DAY_MILLISECONDS;
        generateDefaultCsv(key, expirationTimestamp);
        singleUseKeyService.invalidate(key);

        var records = parseCsv(CSV_HEADERS, csvPath);
        assertDoesNotContainValue(records, SingleUseKeyService.ACCESS_KEY, key);
        assertEquals(3, records.size());
    }

    @Test
    public void testInvalidateWhenKeyIsNotPresent() throws IOException {
        var key = SingleUseKeyService.getKey();
        var expirationTimestamp = System.currentTimeMillis()+ DAY_MILLISECONDS;
        generateDefaultCsv(null, expirationTimestamp);
        singleUseKeyService.invalidate(key);

        var records = parseCsv(CSV_HEADERS, csvPath);
        assertDoesNotContainValue(records, SingleUseKeyService.ACCESS_KEY, key);
        assertEquals(3, records.size());
    }

    @Test
    public void testInvalidateMultipleTimes() throws IOException {
        var key = SingleUseKeyService.getKey();
        var expirationTimestamp = System.currentTimeMillis() + DAY_MILLISECONDS;
        generateDefaultCsv(key, expirationTimestamp);
        var key2 = parseKey(singleUseKeyService.generate(UUID_TEST));
        singleUseKeyService.invalidate(key);
        singleUseKeyService.invalidate(key2);

        var records = parseCsv(CSV_HEADERS, csvPath);
        assertDoesNotContainValue(records, SingleUseKeyService.ACCESS_KEY, key);
        assertDoesNotContainValue(records, SingleUseKeyService.ACCESS_KEY, key2);
        assertEquals(3, records.size());
    }

    private void assertDoesNotContainValue(List<CSVRecord> csvRecords, String column, String value) {
        for (CSVRecord record : csvRecords) {
            if (value.equals(record.get(column))) {
                fail("Entry found!");
                return;
            }
        }
    }

    private void assertCsvContainsCorrectEntry(List<CSVRecord> csvRecords, String id, String key) {
        for (CSVRecord record : csvRecords) {
            if (key.equals(record.get(SingleUseKeyService.ACCESS_KEY))) {
                assertEquals(id, record.get(SingleUseKeyService.ID));
                // timestamp must not be null and must be in the future
                var expirationTimestamp = record.get(SingleUseKeyService.TIMESTAMP);
                assertNotNull(expirationTimestamp);
                assertTrue(Long.parseLong(expirationTimestamp) > System.currentTimeMillis());
                return;
            }
        }
        fail("Correct entry not found");
    }

    private String parseKey(Map<String, String> map) {
        var url = map.get("url");
        var parts = url.split("/");
        var lastIndex = parts.length - 1;
        return parts[lastIndex];
    }

    private void generateDefaultCsv(String key, long expiration) throws IOException {
        try (var csvPrinter = createCsvPrinter(CSV_HEADERS, csvPath)) {
             for (String id : ids) {
                 csvPrinter.printRecord(id, SingleUseKeyService.getKey(), expiration);
             }
             if (key != null) {
                 csvPrinter.printRecord(UUID_TEST, key, expiration);
             }
        }
    }
}
