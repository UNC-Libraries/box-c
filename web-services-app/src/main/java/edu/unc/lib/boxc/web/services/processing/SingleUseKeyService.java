package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static edu.unc.lib.boxc.web.services.utils.CsvUtil.createCsvPrinter;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.parseCsv;

/**
 * Generate and invalidate access keys for single use links
 * @author snluong
 */
public class SingleUseKeyService {
    public static final String ID = "UUID";
    public static final String ACCESS_KEY = "Access Key";
    private static final String TIMESTAMP = "Timestamp";
    public static final String[] CSV_HEADERS = new String[] {ID, ACCESS_KEY, TIMESTAMP};
    private Path csvPath;

    /**
     * Generates an access key for a particular ID, adds it to the CSV, and returns the key
     * @param id UUID of the record
     * @return generated access key
     */
    public String generate(String id) {
        var lock = new ReentrantLock();
        var key = getKey();
        lock.lock();
        try (var csvPrinter = createCsvPrinter(CSV_HEADERS, csvPath)) {
            csvPrinter.printRecord(id, key, System.currentTimeMillis());
        } catch (Exception e) {
            throw new RepositoryException("Failed to write new key to Single Use Key CSV", e);
        } finally {
            lock.unlock();
        }
        return key;
    }

    /**
     * Determines if a key is valid by seeing if it is in the CSV, connected to the proper ID
     * @param id uuid of the box-c record
     * @param key access key for single use link
     * @return true if key is in the CSV, otherwise false
     * @throws IOException
     */
    public boolean keyIsValid(String id, String key) throws IOException {
        var csvRecords = parseCsv(CSV_HEADERS, csvPath);
        for (CSVRecord record : csvRecords) {
            if (accessKeyMatchesUuid(record, id, key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Invalidates a key by removing its entry from the CSV
     * @param key access key of the box-c record
     */
    public void invalidate(String key) {
        var lock = new ReentrantLock();
        lock.lock();
        try {
            var csvRecords = parseCsv(CSV_HEADERS, csvPath);
            var updatedRecords = new ArrayList<>();
            for (CSVRecord record : csvRecords) {
                if (!key.equals(record.get(ACCESS_KEY))) {
                    updatedRecords.add(record);
                }
            }
            try (var csvPrinter = createCsvPrinter(CSV_HEADERS, csvPath)) {
                csvPrinter.flush();
                csvPrinter.printRecords(updatedRecords);
            } catch (Exception e) {
                throw new IOException("Failed rewrite of Single Use Key CSV");
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to invalidate key in Single Use Key CSV", e);
        } finally {
            lock.unlock();
        }

    }

    /**
     * Check that the access key is connected to the uuid in question
     * @param record CSV entry
     * @param uuid uuid of the box-c record
     * @param key access key
     * @return true if they are in the same CSV line
     */
    private boolean accessKeyMatchesUuid(CSVRecord record,String uuid, String key) {
        return uuid.equals(record.get(ID)) && key.equals(record.get(ACCESS_KEY));
    }

    public static String getKey() {
        return UUID.randomUUID().toString().replace("-", "") + Long.toHexString(System.nanoTime());
    }

    public void setCsvPath(Path csvPath) {
        this.csvPath = csvPath;
    }
}
