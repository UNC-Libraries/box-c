package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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
@Configuration
@EnableScheduling
public class SingleUseKeyService {
    public static final String ID = "UUID";
    public static final String ACCESS_KEY = "Access Key";
    public static final String TIMESTAMP = "Expiration Timestamp";
    public static final String[] CSV_HEADERS = new String[] {ID, ACCESS_KEY, TIMESTAMP};
    public static final long DAY_MILLISECONDS = 86400000;
    public static final String KEY = "key";
    private Path csvPath;
    private ReentrantLock lock = new ReentrantLock();
    private static final Logger log = LoggerFactory.getLogger(SingleUseKeyService.class);

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
            for (CSVRecord row : csvRecords) {
                if (key.equals(row.get(ACCESS_KEY))) {
                    var expirationTimestamp = Long.parseLong(row.get(TIMESTAMP));
                    return currentMilliseconds <= expirationTimestamp;
                }
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to determine if key is valid in Single Use Key CSV", e);
        }
        return false;
    }

    /**
     * Invalidates a single use key CSV record by removing it from the CSV
     * @param key access key of the box-c record, may be null for time-based invalidation
     */
    public void invalidate(String key) {
        lock.lock();
        try {
            var csvRecords = parseCsv(CSV_HEADERS, csvPath);
            var updatedRecords = new ArrayList<>();
            var recordsChanged = false;
            for (CSVRecord row : csvRecords) {
                if (recordShouldBeInvalidated(key, row)) {
                    recordsChanged = true;
                } else {
                    // keep this record as it is valid
                    updatedRecords.add(row);
                }
            }

            if (recordsChanged) {
                writeNewCsv(updatedRecords);
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to invalidate record in Single Use Key CSV", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Determines if the specific record should be invalidated
     * @param key the access key, may be null if we want a time-based eval
     * @param row the csv row that is the CSV record
     * @return true if the record should be invalidated
     */
    private boolean recordShouldBeInvalidated(String key, CSVRecord row) {
        if (key == null) {
            // if record's timestamp is in the past, it should be invalidated
            var currentTime = System.currentTimeMillis();
            return Long.parseLong(row.get(TIMESTAMP)) < currentTime;
        } else {
            // if the record's key matches, it should be invalidated
            return key.equals(row.get(ACCESS_KEY));
        }
    }

    /**
     * We are running a cron job every hour to invalidate any expired single use keys
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduleInvalidation() {
        log.info("Invalidate Single Use Key Cron is running!");
        invalidate(null);
    }

    private void writeNewCsv(ArrayList<Object> records) throws IOException {
        try (var csvPrinter = createNewCsvPrinter(CSV_HEADERS, csvPath)) {
            csvPrinter.flush();
            csvPrinter.printRecords(records);
        }
    }

    public static String getKey() {
        return UUID.randomUUID().toString().replace("-", "") + Long.toHexString(System.nanoTime());
    }

    public String getId(String key) throws IOException {
        var csvRecords = parseCsv(CSV_HEADERS, csvPath);
        for (CSVRecord row : csvRecords) {
            if (key.equals(row.get(ACCESS_KEY))) {
                return row.get(ID);
            }
        }
        return null;
    }

    private Map<String, String> keyToMap(String key, String id, long expirationTimestamp) {
        Map<String, String> result = new HashMap<>();
        result.put(KEY, key);
        result.put("target_id", id);
        result.put("expires", String.valueOf(expirationTimestamp));

        return result;
    }

    public void setCsvPath(Path csvPath) {
        this.csvPath = csvPath;
    }
}
