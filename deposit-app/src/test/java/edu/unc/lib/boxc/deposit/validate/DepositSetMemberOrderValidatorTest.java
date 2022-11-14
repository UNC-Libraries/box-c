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
package edu.unc.lib.boxc.deposit.validate;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.order.OrderValidator;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author bbpennel
 */
public class DepositSetMemberOrderValidatorTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String CHILD3_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";

    private Model depositModel;
    private PID parentPid;
    private PID child1Pid;
    private PID child2Pid;
    private DepositSetMemberOrderValidator validator;
    private Bag workBag;

    @Before
    public void setup() {
        parentPid = PIDs.get(PARENT_UUID);
        child1Pid = PIDs.get(CHILD1_UUID);
        child2Pid = PIDs.get(CHILD2_UUID);

        depositModel = ModelFactory.createDefaultModel();
        workBag = depositModel.createBag(parentPid.getRepositoryPath());
        workBag.addProperty(RDF.type, Cdr.Work);
        var child1 = depositModel.getResource(child1Pid.getRepositoryPath());
        child1.addProperty(RDF.type, Cdr.FileObject);
        var child2 = depositModel.getResource(child2Pid.getRepositoryPath());
        child2.addProperty(RDF.type, Cdr.FileObject);
        workBag.add(child1);
        workBag.add(child2);

        validator = new DepositSetMemberOrderValidator();
        validator.setResource(workBag);
    }

    @Test
    public void validMemberOrderTest() throws Exception {
        workBag.addProperty(Cdr.memberOrder,CHILD1_UUID + "|" + CHILD2_UUID);
        assertTrue(validator.isValid());
        assertTrue(validator.getErrors().isEmpty());
    }

    @Test
    public void resourceIsNotAWorkTest() {
        var folderId = "8dd13ef6-1011-4acc-9f2f-ac1cdf03d800";
        var folderPid = PIDs.get(folderId);
        var folderBag = depositModel.createBag(folderPid.getRepositoryPath());
        folderBag.addProperty(Cdr.memberOrder,CHILD1_UUID + "|" + CHILD2_UUID);
        folderBag.addProperty(RDF.type, Cdr.Folder);
        var newValidator = new DepositSetMemberOrderValidator();
        newValidator.setResource(folderBag);

        assertFalse(newValidator.isValid());
        assertFalse(newValidator.getErrors().isEmpty());
        assertHasErrors(
            newValidator,
            "Object " + folderId + " does not support member ordering");
    }

    @Test
    public void memberOrderContainsNonChildrenTest() throws Exception {
        workBag.addProperty(Cdr.memberOrder,CHILD1_UUID + "|" + CHILD2_UUID + "|" + CHILD3_UUID);
        assertFalse(validator.isValid());
        assertFalse(validator.getErrors().isEmpty());
        assertHasErrors(
            validator,
            "Invalid member order for " + PARENT_UUID
                    + ", the following IDs are not members: " + CHILD3_UUID);
    }

    @Test
    public void memberOrderMissingChildrenTest() {
        workBag.addProperty(Cdr.memberOrder, CHILD1_UUID);
        assertFalse(validator.isValid());
        assertFalse(validator.getErrors().isEmpty());
        assertHasErrors(
            validator,
            "Invalid member order for " + PARENT_UUID
                    + ", the following members were expected but not listed: " + CHILD2_UUID);
    }

    @Test
    public void memberOrderHasDuplicateChildrenTest() {
        workBag.addProperty(Cdr.memberOrder, CHILD1_UUID + "|" + CHILD2_UUID + "|" + CHILD2_UUID);
        assertFalse(validator.isValid());
        assertFalse(validator.getErrors().isEmpty());
        assertHasErrors(
                validator,
                "Invalid member order for " + PARENT_UUID
                        + ", it contained duplicate member IDs: " + CHILD2_UUID);
    }

    @Test
    public void memberOrderMultipleErrorTest() {
        workBag.addProperty(Cdr.memberOrder, CHILD1_UUID + "|" + CHILD3_UUID);
        assertFalse(validator.isValid());
        assertFalse(validator.getErrors().isEmpty());
        assertHasErrors(
                validator,
                "Invalid member order for " + PARENT_UUID
                        + ", the following members were expected but not listed: " + CHILD2_UUID,
                "Invalid member order for " + PARENT_UUID
                        + ", the following IDs are not members: " + CHILD3_UUID
                );
    }

    public static void assertHasErrors(OrderValidator validator, String... expected) {
        var msg = "Expected errors:\n[" + String.join(",", expected) + "]\nbut errors were:\n" + validator.getErrors();
        assertTrue(msg, validator.getErrors().containsAll(Arrays.asList(expected)));
        assertEquals(msg, expected.length, validator.getErrors().size());
    }
}
