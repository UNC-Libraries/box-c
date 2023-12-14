package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Generate and invalidate access keys for single use links
 * @author snluong
 */
public class SingleUseKeyService {
    private static final String ID = "UUID";
    private static final String ACCESS_KEY = "Access Key";
    private static final String TIMESTAMP = "Timestamp";
    private static final String[] CSV_HEADERS = new String[] {ID, ACCESS_KEY, TIMESTAMP};
    private Path csvPath;
    public String generate(String id) {
        var lock = new ReentrantLock();
        var key = getKey();
        lock.lock();
        try (var csvPrinter = createCsvPrinter(csvPath)) {
            csvPrinter.printRecord(id, key, System.currentTimeMillis());
        } catch (Exception e) {
            throw new RepositoryException("Failed to write new key to Single Use Key CSV", e);
        } finally {
            lock.unlock();
        }
        return key;
    }
    public boolean keyIsValid(String key) throws IOException {
        var csvRecords = parseCsv(csvPath);
        for (CSVRecord record : csvRecords) {
            if (key.equals(record.get(ACCESS_KEY))) {
                return true;
            }
        }
        return false;
    }
    public void invalidate(String key) {
        var lock = new ReentrantLock();
        lock.lock();
        try {
            List<CSVRecord> csvRecords = parseCsv(csvPath);
            var updatedRecords = new ArrayList<>();
            for (CSVRecord record : csvRecords) {
                if (!key.equals(record.get(ACCESS_KEY))) {
                    updatedRecords.add(record);
                }
            }
//            try (var csvPrinter = createCsvPrinter(csvPath)) {
//                csvPrinter.flush();
//                csvPrinter.printRecords(updatedRecords);
//            } catch (Exception e) {
//                throw new IOException("Failed rewrite of Single Use Key CSV");
//            }
//            try (FileWriter writer = new FileWriter(csvPath.toFile())) {
//
//            }

        } catch (IOException e) {
            throw new RepositoryException("Failed to invalidate key in Single Use Key CSV", e);
        } finally {
            lock.unlock();
        }

    }
    private CSVPrinter createCsvPrinter(Path csvPath) throws IOException {
        var writer = Files.newBufferedWriter(csvPath);
        return new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader(CSV_HEADERS));
    }

    private List<CSVRecord> parseCsv(Path csvPath) throws IOException {
        Reader reader = Files.newBufferedReader(csvPath);
        return new CSVParser(reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withHeader(CSV_HEADERS)
                .withTrim())
                .getRecords();
    }
    private String getKey() {
        return UUID.randomUUID().toString().replace("-", "") + Long.toHexString(System.nanoTime());
    }

    public void setCsvPath(Path csvPath) {
        this.csvPath = csvPath;
    }
}
