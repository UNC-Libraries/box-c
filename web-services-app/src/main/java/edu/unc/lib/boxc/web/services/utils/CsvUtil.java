package edu.unc.lib.boxc.web.services.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Util for CSV-related operations
 * @author snluong
 */
public class CsvUtil {
    public static List<CSVRecord> parseCsv(String[] headers, Path csvPath) throws IOException {
        Reader reader = Files.newBufferedReader(csvPath);
        return new CSVParser(reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withHeader(headers)
                .withTrim())
                .getRecords();
    }

    /**
     * Creates a CSV printer and determines whether new rows should be appended to existing CSV
     * @param headers header values of the CSV
     * @param csvPath path of the CSV
     * @return
     * @throws IOException
     */
    public static CSVPrinter createCsvPrinter(String[] headers, Path csvPath) throws IOException {
        var file = csvPath.toFile();
        if (file.exists()) {
            var writer = Files.newBufferedWriter(csvPath, StandardOpenOption.APPEND);
            return new CSVPrinter(writer, CSVFormat.DEFAULT
                    .withHeader(headers));
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
}
