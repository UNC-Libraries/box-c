/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.unc.lib.boxc.web.services.processing.MemberOrderCsvConstants.CSV_HEADERS;
import static edu.unc.lib.boxc.web.services.processing.MemberOrderCsvConstants.ORDER_HEADER;
import static edu.unc.lib.boxc.web.services.processing.MemberOrderCsvConstants.PARENT_PID_HEADER;
import static edu.unc.lib.boxc.web.services.processing.MemberOrderCsvConstants.PID_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author bbpennel
 */
public class MemberOrderCsvTransformerTest {
    private static final String PARENT1_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String PARENT2_UUID = "ba70a1ee-fa7c-437f-a979-cc8b16599652";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String CHILD3_UUID = "8e0040b2-9951-48a3-9d65-780ae7106951";
    private static final String CHILD4_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    private MemberOrderCsvTransformer transformer;

    @Before
    public void setup() {
        transformer = new MemberOrderCsvTransformer();
    }

    @Test
    public void toSetRequestNoParentIdTest() throws Exception {
        var entries = new ArrayList<List<Object>>();
        entries.add(Arrays.asList("", CHILD1_UUID, "Title 1", ResourceType.File.name(),
                "file.txt", "text/plain", false, 1));
        var csvPath = writeCsvFile(entries);
        try {
            transformer.toRequest(csvPath);
            fail();
        } catch (IllegalArgumentException e) {
            assertErrorMessageContains(e, "does not specify a value for required field 'Parent PID'");
        }
    }

    @Test
    public void toSetRequestNoChildIdTest() throws Exception {
        var entries = new ArrayList<List<Object>>();
        entries.add(Arrays.asList(PARENT1_UUID, "", "Title 1", ResourceType.File.name(),
                "file.txt", "text/plain", false, 1));
        var csvPath = writeCsvFile(entries);
        try {
            transformer.toRequest(csvPath);
            fail();
        } catch (IllegalArgumentException e) {
            assertErrorMessageContains(e, "does not specify a value for required field 'PID'");
        }
    }

    @Test
    public void toSetRequestParentNotPidTest() throws Exception {
        var entries = new ArrayList<List<Object>>();
        entries.add(Arrays.asList("whatever", CHILD1_UUID, "Title 1", ResourceType.File.name(),
                "file.txt", "text/plain", false, 1));
        var csvPath = writeCsvFile(entries);
        try {
            transformer.toRequest(csvPath);
            fail();
        } catch (IllegalArgumentException e) {
            assertErrorMessageContains(e, "contains an invalid PID value for field 'Parent PID'");
        }
    }

    @Test
    public void toSetRequestNoOrderIdTest() throws Exception {
        var entries = new ArrayList<List<Object>>();
        entries.add(Arrays.asList(PARENT1_UUID, CHILD1_UUID, "Title 1", ResourceType.File.name(),
                "file.txt", "text/plain", false, ""));
        var csvPath = writeCsvFile(entries);
        try {
            transformer.toRequest(csvPath);
            fail();
        } catch (IllegalArgumentException e) {
            assertErrorMessageContains(e, "does not specify a value for required field 'Member Order'");
        }
    }

    @Test
    public void toSetRequestOrderIdNotANumberTest() throws Exception {
        var entries = new ArrayList<List<Object>>();
        entries.add(Arrays.asList(PARENT1_UUID, CHILD1_UUID, "Title 1", ResourceType.File.name(),
                "file.txt", "text/plain", false, "hmm"));
        var csvPath = writeCsvFile(entries);
        try {
            transformer.toRequest(csvPath);
            fail();
        } catch (IllegalArgumentException e) {
            assertErrorMessageContains(e, "invalid value for field 'Member Order', it must be an integer");
        }
    }

    @Test
    public void toSetRequestOrderIdNegativeTest() throws Exception {
        var entries = new ArrayList<List<Object>>();
        entries.add(Arrays.asList(PARENT1_UUID, CHILD1_UUID, "Title 1", ResourceType.File.name(),
                "file.txt", "text/plain", false, -4));
        var csvPath = writeCsvFile(entries);
        try {
            transformer.toRequest(csvPath);
            fail();
        } catch (IllegalArgumentException e) {
            assertErrorMessageContains(e, "invalid value for field 'Member Order', it must be >= 0");
        }
    }

    @Test
    public void toSetRequestMultipleEntriesTest() throws Exception {
        var entries = new ArrayList<List<Object>>();
        entries.add(Arrays.asList(PARENT1_UUID, CHILD1_UUID, "Title 1", ResourceType.File.name(),
                "file1.txt", "text/plain", false, 2));
        entries.add(Arrays.asList(PARENT1_UUID, CHILD2_UUID, "Title 2", ResourceType.File.name(),
                "file2.txt", "text/plain", false, 1));
        entries.add(Arrays.asList(PARENT1_UUID, CHILD3_UUID, "Title 3", ResourceType.File.name(),
                "file3.txt", "text/plain", true, 5));
        entries.add(Arrays.asList(PARENT2_UUID, CHILD4_UUID, "Title 4", ResourceType.File.name(),
                "file4.txt", "text/plain", false, 0));
        var csvPath = writeCsvFile(entries);

        var request = transformer.toRequest(csvPath);
        assertEquals(OrderOperationType.SET, request.getOperation());
        var parentToOrder = request.getParentToOrdered();
        var parent1Children = parentToOrder.get(PARENT1_UUID);
        assertEquals(Arrays.asList(CHILD2_UUID, CHILD1_UUID, CHILD3_UUID), parent1Children);
        var parent2Children = parentToOrder.get(PARENT2_UUID);
        assertEquals(Arrays.asList(CHILD4_UUID), parent2Children);
    }

    @Test
    public void toSetRequestMinimalColumnsTest() throws Exception {
        var entries = new ArrayList<List<Object>>();
        entries.add(Arrays.asList(PARENT1_UUID, CHILD1_UUID, 2));
        entries.add(Arrays.asList(PARENT1_UUID, CHILD2_UUID, 1));
        var csvPath = writeCsvFile(entries, PARENT_PID_HEADER, PID_HEADER, ORDER_HEADER);

        var request = transformer.toRequest(csvPath);
        assertEquals(OrderOperationType.SET, request.getOperation());
        var parentToOrder = request.getParentToOrdered();
        var parent1Children = parentToOrder.get(PARENT1_UUID);
        assertEquals(Arrays.asList(CHILD2_UUID, CHILD1_UUID), parent1Children);
    }

    private void assertErrorMessageContains(Exception e, String expected) {
        assertTrue("Expected message:\n" + expected + "\nReceived:\n" + e.getMessage(),
                e.getMessage().contains(expected));
    }

    private Path writeCsvFile(List<List<Object>> entries) throws IOException {
        return writeCsvFile(entries, CSV_HEADERS);
    }

    private Path writeCsvFile(List<List<Object>> entries, String... headers) throws IOException {
        var csvPath = tmpFolder.newFile().toPath();
        try (var writer = Files.newBufferedWriter(csvPath);
            var csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader(headers))) {
            for (var entry: entries) {
                csvPrinter.printRecord(entry);
            }
        }
        return csvPath;
    }
}
