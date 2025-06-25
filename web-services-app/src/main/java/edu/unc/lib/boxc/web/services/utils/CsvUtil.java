package edu.unc.lib.boxc.web.services.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.io.File.createTempFile;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

/**
 * Util for CSV-related operations
 * @author snluong
 */
public class CsvUtil {
    private static final Logger log = LoggerFactory.getLogger(CsvUtil.class);
    public static final String PID_HEADER = "workId";
    public static final String REF_ID_HEADER = "refId";
    private CsvUtil(){
    }

    public static List<CSVRecord> parseCsv(String[] headers, Path csvPath) throws IOException {
        Reader reader = Files.newBufferedReader(csvPath);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .setSkipHeaderRecord(true)
                .get();
        return CSVParser.parse(reader, format).getRecords();
    }

    /**
     * Creates a CSV printer and determines whether new rows should be appended to existing CSV
     * @param headers header values of the CSV
     * @param csvPath path of the CSV
     * @return
     * @throws IOException
     */
    public static CSVPrinter createCsvPrinter(String[] headers, Path csvPath) throws IOException {
        if (Files.exists(csvPath)) {
            var writer = Files.newBufferedWriter(csvPath, StandardOpenOption.APPEND);
            return new CSVPrinter(writer, CSVFormat.DEFAULT.withSkipHeaderRecord());
        } else {
            return createNewCsvPrinter(headers, csvPath);
        }
    }

    /**
     * Make a new CSV printer that does not append new rows
     * @param headers header values of the CSV
     * @param csvPath path of the CSV
     * @return
     * @throws IOException
     */
    public static CSVPrinter createNewCsvPrinter(String[] headers, Path csvPath) throws IOException {
        var writer = Files.newBufferedWriter(csvPath);
        return new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader(headers));
    }

    /**
     * Deletes CSV temp file
     * @param csvPath path where CSV is located
     */
    public static void cleanupCsv(Path csvPath) {
        if (csvPath != null) {
            try {
                Files.deleteIfExists(csvPath);
            } catch (IOException e) {
                log.warn("Failed to cleanup CSV file: " + e.getMessage());
            }
        }
    }

    /**
     * Stores CSV from controller request in temp folder
     * @param csvFile CSV file to store
     * @param type specifies "order" for member order or "refId" for Aspace ref IDs
     * @return path of temp csv file
     * @throws IOException
     */
    public static Path storeCsvToTemp(MultipartFile csvFile, String type) throws IOException {
        File importFile = createTempFile("import_" + type, ".xml");
        copyInputStreamToFile(csvFile.getInputStream(), importFile);
        return importFile.toPath();
    }

    public static Map<String, String> convertCsvToMap(String[] headers, Path csvPath) throws IOException {
        Map<String, String> result = new HashMap<>();
        var csvRows = parseCsv(headers, csvPath);
        for (CSVRecord row : csvRows) {
            result.put(row.get(PID_HEADER), row.get(REF_ID_HEADER));
        }
        return result;
    }
}
