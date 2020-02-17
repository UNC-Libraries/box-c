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
package edu.unc.lib.deposit.fcrepo4;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Job which registers deposited files to longleaf
 *
 * @author smithjp
 *
 */
public class RegisterToLongleafJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory.getLogger(RegisterToLongleafJob.class);

    public RegisterToLongleafJob() {
        super();
    }

    public RegisterToLongleafJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        log.debug("Registering files from deposit {} to longleaf", getDepositPID());

        Model model = getReadOnlyModel();
        Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());

        registerFilesToLongleaf(depositBag);
    }

    public void registerFilesToLongleaf(Resource resc) {
        Set<Resource> rescTypes = resc.listProperties(RDF.type).toList().stream()
                .map(Statement::getResource).collect(toSet());

        System.out.println(rescTypes);

        if (rescTypes.contains(Cdr.FileObject)) {
            System.out.println("object is file");
            if (resc.hasProperty(CdrDeposit.storageUri)) {
                System.out.println("file has been transferred");
            } else if (resc.hasProperty(CdrDeposit.descriptiveStorageUri)) {
                System.out.println("mods file has been transferred");
            } else {
                System.out.println("file has not been transferred");
            }
        } else {
            System.out.println("object is not file");
        }

        System.out.println(resc.hasProperty(CdrDeposit.stagingLocation));
        System.out.println(resc.hasProperty(CdrDeposit.storageUri));

        NodeIterator iterator = getChildIterator(resc);
        // No more children, nothing further to do in this tree
        if (iterator == null) {
            return;
        }

        try {
            while (iterator.hasNext()) {
                Resource childResc = (Resource) iterator.next();
                registerFilesToLongleaf(childResc);
            }
        } finally {
            iterator.close();
        }
    }
}
