package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.model.api.exceptions.InvalidPidException;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.order.MultiParentOrderRequest;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for transforming Member Order CSV to requests
 *
 * @author bbpennel
 */
public class MemberOrderCsvTransformer {
    public static String MEMBER_ORDER_INVALID = "Import has invalid member order";
    /**
     * Transform the provided CSV document into a request for setting the order of members of one or more containers.
     *
     * The CSV must contain at least the parent id, child id, and order id fields. Other fields are ignored.
     *
     * IllegalArgumentExceptions will be thrown if the values in the CSV are missing or invalid.
     * @param csvPath Path to CSV document
     * @return constructed request
     * @throws IOException if the CSV cannot be parsed
     */
    public MultiParentOrderRequest toRequest(Path csvPath) throws IOException {
        var parentToChildren = parseCsvToMapping(csvPath);
        var validImport = validateImport(parentToChildren);
        if (!validImport) {
            throw new IllegalArgumentException(MEMBER_ORDER_INVALID);
        }
        var parentToOrdered = sortParentToChildren(parentToChildren);
        var request = new MultiParentOrderRequest();
        request.setParentToOrdered(parentToOrdered);
        request.setOperation(OrderOperationType.SET);
        return request;
    }

    // Parses the provided CSV file to produce a mapping of parent ids to children, in the order they appear
    private static Map<String, List<SortableChildEntry>> parseCsvToMapping(Path csvPath) throws IOException {
        var parentToChildren = new HashMap<String, List<SortableChildEntry>>();
        try (var csvParser = createCsvParser(csvPath)) {
            for (CSVRecord csvRecord : csvParser) {
                var parentId = getRequiredPidValue(csvRecord, MemberOrderCsvConstants.PARENT_PID_HEADER);
                var childId = getRequiredPidValue(csvRecord, MemberOrderCsvConstants.PID_HEADER);
                var orderId = getOrderId(csvRecord);
                var children = parentToChildren.computeIfAbsent(parentId, x -> new ArrayList<>());
                if (orderId == null) {
                    children.add(null);
                } else {
                    children.add(new SortableChildEntry(childId, orderId));
                }
            }
        }
        return parentToChildren;
    }

    // Produces a map where keys are the parent id and values are lists of children ids sorted by the provided order id
    private static Map<String, List<String>> sortParentToChildren(
            Map<String, List<SortableChildEntry>> parentToUnsorted) {
        var parentToChildren = new HashMap<String, List<String>>();
        for (var parentEntry: parentToUnsorted.entrySet()) {
            // Sort the children entries and get a list of children ids
            var values = parentEntry.getValue();
            List<String> sorted = null;
            if (!values.contains(null)) {
                sorted = values.stream()
                        .sorted(Comparator.comparingInt(c -> c.orderId))
                        .map(c -> c.childId)
                        .collect(Collectors.toList());

            }
            parentToChildren.put(parentEntry.getKey(), sorted);
        }
        return parentToChildren;
    }

    // Object to allow for sorting of children by their order ids
    private static class SortableChildEntry {
        private String childId;
        private Integer orderId;

        SortableChildEntry(String childId, Integer orderId) {
            this.childId = childId;
            this.orderId = orderId;
        }
    }

    // checks each parent's child entries to make sure they're either all nulls or all SortableChildEntry objects
    private boolean validateImport(Map<String, List<SortableChildEntry>> importMapping) {
        for (var parentEntry: importMapping.entrySet()) {
            var values = parentEntry.getValue();
            var includesNull = values.contains(null);
            var includesSortableEntry = false;
            for (var value : values) {
                includesSortableEntry = value != null;
                if (includesSortableEntry) {
                    break;
                }
            }
            if (includesNull && includesSortableEntry) {
                return false;
            }
        }
        return true;
    }

    private static String getRequiredValue(CSVRecord csvRecord, String fieldName) {
        var value = csvRecord.get(fieldName);
        if (StringUtils.isBlank(value)) {
            throw invalidRecordError(csvRecord, "does not specify a value for required field '" + fieldName + "'");
        }
        return value;
    }

    private static String getRequiredPidValue(CSVRecord csvRecord, String fieldName) {
        var value = getRequiredValue(csvRecord, fieldName);
        // Verifies that the value is a PID, and grab just the id
        try {
            return PIDs.get(value).getId();
        } catch (InvalidPidException e) {
            throw invalidRecordError(csvRecord, "contains an invalid PID value for field '" + fieldName + "'.");
        }
    }

    private static Integer getOrderId(CSVRecord csvRecord) {
        var fieldName = MemberOrderCsvConstants.ORDER_HEADER;
//        var value = getRequiredValue(csvRecord, fieldName);
        var value = csvRecord.get(fieldName);
        try {
            if (!value.isBlank()) {
                var intVal = Integer.parseInt(value);
                if (intVal < 0) {
                    throw invalidRecordError(csvRecord,
                            "contains an invalid value for field '" + fieldName + "', it must be >= 0.");
                }
                return intVal;
            }
            return null;
        } catch (NumberFormatException e) {
            throw invalidRecordError(csvRecord,
                    "contains an invalid value for field '" + fieldName + "', it must be an integer.");
        }
    }

    private static IllegalArgumentException invalidRecordError(CSVRecord csvRecord, String message) {
        return new IllegalArgumentException("Record " + csvRecord.getRecordNumber()
                + " " + message);
    }

    private static CSVParser createCsvParser(Path csvPath) throws IOException {
        Reader reader = Files.newBufferedReader(csvPath);
        return new CSVParser(reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withAllowMissingColumnNames()
                .withTrim());
    }
}
