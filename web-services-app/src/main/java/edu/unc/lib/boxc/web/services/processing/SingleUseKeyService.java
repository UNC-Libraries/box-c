package edu.unc.lib.boxc.web.services.processing;

import com.fasterxml.jackson.databind.JsonNode;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static edu.unc.lib.boxc.web.services.utils.CsvUtil.createCsvPrinter;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.createNewCsvPrinter;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.parseCsv;

/**
 * Generate and invalidate access keys for single use links
 * @author snluong
 */
public class SingleUseKeyService {
    public static final String ID = "UUID";
    public static final String ACCESS_KEY = "Access Key";
    public static final String TIMESTAMP = "Expiration Timestamp";
    public static final String[] CSV_HEADERS = new String[] {ID, ACCESS_KEY, TIMESTAMP};
    public static final long DAY_MILLISECONDS = 86400000;
    private Path csvPath;
    private ReentrantLock lock = new ReentrantLock();

    /**
     * Generates an access key for a particular ID, adds it to the CSV, and returns the key
     * @param id UUID of the record
     * @return generated access key
     */
    public Map<String, String> generate(String id) {
        var key = getKey();
        lock.lock();
        var expirationInMilliseconds = System.currentTimeMillis() + DAY_MILLISECONDS;
        try (var csvPrinter = createCsvPrinter(CSV_HEADERS, csvPath)) {
            csvPrinter.printRecord(id, key, expirationInMilliseconds);
        } catch (Exception e) {
            throw new RepositoryException("Failed to write new key to Single Use Key CSV", e);
        } finally {
            lock.unlock();
        }
        return keyToMap(key, id, expirationInMilliseconds);
    }

    /**
     * Determines if a key is valid by seeing if it is in the CSV and if the expiration timestamp has not passed
     * @param key access key for single use link
     * @return true if key is in the CSV, otherwise false
     */
    public boolean keyIsValid(String key) {
        try {
            var csvRecords = parseCsv(CSV_HEADERS, csvPath);
            var currentMilliseconds = System.currentTimeMillis();
            for (CSVRecord record : csvRecords) {
                if (key.equals(record.get(ACCESS_KEY))) {
                    var expirationTimestamp = Long.parseLong(record.get(TIMESTAMP));
                    return currentMilliseconds <= expirationTimestamp;
                }
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to determine if key is valid in Single Use Key CSV", e);
        }
        return false;
    }

    /**
     * Invalidates a key by removing its entry from the CSV
     * @param key access key of the box-c record
     */
    public void invalidate(String key) {
        lock.lock();
        try {
            var csvRecords = parseCsv(CSV_HEADERS, csvPath);
            var updatedRecords = new ArrayList<>();
            var keyExists = false;
            for (CSVRecord record : csvRecords) {
                if (key.equals(record.get(ACCESS_KEY))) {
                    keyExists = true;
                } else {
                    // add the rest of the keys to list
                    updatedRecords.add(record);
                }
            }

            if (keyExists) {
                try (var csvPrinter = createNewCsvPrinter(CSV_HEADERS, csvPath)) {
                    csvPrinter.flush();
                    csvPrinter.printRecords(updatedRecords);
                }
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to invalidate key in Single Use Key CSV", e);
        } finally {
            lock.unlock();
        }
    }

    public static String getKey() {
        return UUID.randomUUID().toString().replace("-", "") + Long.toHexString(System.nanoTime());
    }

    private Map<String, String> keyToMap(String key, String id, long expirationTimestamp) {
        Map<String, String> result = new HashMap<>();
        result.put("url", "services/api/single_use_link/" + key);
        result.put("target_id", id);
        result.put("expires", String.valueOf(expirationTimestamp));

        return result;
    }

    public void setCsvPath(Path csvPath) {
        this.csvPath = csvPath;
    }
}
