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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

/**
 * @author bbpennel
 */
public class DepositSetMemberOrderValidatorTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";

    private Model depositModel;
    private PID parentPid;
    private PID child1Pid;
    private PID child2Pid;
    private DepositSetMemberOrderValidator validator;

    @Before
    public void setup() {
        parentPid = PIDs.get(PARENT_UUID);
        child1Pid = PIDs.get(CHILD1_UUID);
        child2Pid = PIDs.get(CHILD2_UUID);

        depositModel = ModelFactory.createDefaultModel();
        var workBag = depositModel.createBag(parentPid.getRepositoryPath());
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

    // not a work
    // order list contains ids that are not children
    // not all the children are accounted for in the order list
    // member order is valid
    // duplicate ids in the order list

    @Test
    public void validMemberOrderTest() throws Exception {

    }
}
