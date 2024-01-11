package edu.unc.lib.boxc.web.services.utils;

import edu.unc.lib.boxc.web.services.processing.SingleUseKeyService;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static edu.unc.lib.boxc.web.services.processing.SingleUseKeyService.CSV_HEADERS;
import static edu.unc.lib.boxc.web.services.utils.CsvUtil.createCsvPrinter;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Util class for generating CSVs for single use links tests
 *
 * @author snluong
 */
public class SingleUseKeyUtil {

    public static final String UUID_1 = "f277bb38-272c-471c-a28a-9887a1328a1f";
    public static final String UUID_2 = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    public static final String UUID_3 = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    public static final String UUID_TEST = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String[] ID_ARRAY = {UUID_1, UUID_2, UUID_3};

    public static void generateDefaultCsv(Path csvPath, String key, long expiration) throws IOException {
        try (var csvPrinter = createCsvPrinter(CSV_HEADERS, csvPath)) {
            for (String id : ID_ARRAY) {
                csvPrinter.printRecord(id, SingleUseKeyService.getKey(), expiration);
            }
            if (key != null) {
                csvPrinter.printRecord(UUID_TEST, key, expiration);
            }
        }
    }
}
