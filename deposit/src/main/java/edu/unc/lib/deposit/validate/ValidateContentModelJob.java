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
package edu.unc.lib.deposit.validate;

import static edu.unc.lib.dl.rdf.CdrDeposit.stagingLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.reasoner.ValidityReport.Report;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.acl.fcrepo4.ContentObjectAccessRestrictionValidator;
import edu.unc.lib.dl.rdf.Cdr;

/**
 * Verify that the content objects in this deposit are valid according to our schema and expected field usage.
 *
 * @author bbpennel
 *
 */
public class ValidateContentModelJob extends AbstractDepositJob{
    private static final Logger log = LoggerFactory.getLogger(ValidateContentModelJob.class);

    private final Set<Resource> validObjectTypes;

    private ContentObjectAccessRestrictionValidator aclValidator;

    private final Model cdrSchema;
    private final Reasoner reasoner;

    /**
     * Constructs a new validation job with no job uuid or deposit uuid
     */
    public ValidateContentModelJob() {
        this(null, null);
    }

    /**
     * Constructs a new validation job
     *
     * @param uuid job uuid
     * @param depositUUID deposit uuid
     */
    public ValidateContentModelJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);

        validObjectTypes = new HashSet<>(Arrays.asList(
                Cdr.FileObject, Cdr.Work, Cdr.Folder, Cdr.Collection, Cdr.AdminUnit));

        cdrSchema = FileManager.get().loadModel("cdr-schema.ttl");

        reasoner = ReasonerRegistry.getOWLMicroReasoner().bindSchema(cdrSchema);
    }

    @Override
    public void runJob() {

        Model model = getReadOnlyModel();
        Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());

        // Validate all objects in the deposit model against the schema
        validateSchema(model);

        validatePrimaryObjects(model);

        // Validate each object in this deposit for ACL issues and others not covered by the schema
        validateChildren(depositBag);
    }

    private void validatePrimaryObjects(Model model) {

        StmtIterator it = model.listStatements((Resource) null, Cdr.primaryObject, (RDFNode) null);
        while (it.hasNext()) {
            Statement stmt = it.next();

            Resource subj = stmt.getSubject();
            if (!subj.hasProperty(RDF.type, Cdr.Work)) {
                failJob(null, "Invalid property on {0}, only cdr:Work objects may contain primaryObject relations.",
                        subj.getURI());
            }

            Resource obj = stmt.getResource();
            if (!obj.hasProperty(RDF.type, Cdr.FileObject)) {
                failJob(null, "Invalid primary object defined on {0}, object {1} is not a cdr:FileObject.",
                        subj.getURI(), obj.getURI());
            }
        }
    }

    /**
     * Validates the model against the CDR RDF schema. Mostly catches if
     * properties reference literals of the wrong type
     *
     * @param model
     */
    private void validateSchema(Model model) {
        InfModel infmodel = ModelFactory.createInfModel(reasoner, model);

        ValidityReport validity = infmodel.validate();

        if (validity.isValid()) {
            log.debug("Deposit was valid with regards to schema");
            return;
        } else {
            List<String> errors = new ArrayList<>();
            for (Iterator<Report> i = validity.getReports(); i.hasNext(); ) {
                errors.add(i.next().getDescription());
            }

            failJob(null, "Deposit model was not valid with regards to the schema due to the following issues:\n {0}",
                    String.join("\n", errors));
        }
    }

    private void validateChildren(Resource parentResc) {
        NodeIterator iterator = getChildIterator(parentResc);
        if (iterator == null) {
            return;
        }

        if (parentResc.hasProperty(RDF.type, Cdr.FileObject)) {
            failJob(null, "FileObject {0} cannot contain children, but was provided as a container",
                    parentResc.getURI());
        }

        try {
            while (iterator.hasNext()) {
                Resource childResc = (Resource) iterator.next();

                Resource objType = getValidObjectTypeResc(childResc);
                if (objType == null) {
                    failJob(null, "Could not determine the type of object {0}", childResc.getURI());
                }

                if (childResc.hasProperty(RDF.type, Cdr.FileObject)) {
                    validateFileObject(childResc, parentResc);
                }

                // Verify that ACL assignments for this object are okay
                aclValidator.validate(childResc);

                // Validate any children of this resource
                validateChildren(childResc);
            }
        } finally {
            iterator.close();
        }
    }

    private Resource getValidObjectTypeResc(Resource resc) {
        StmtIterator typeIt = resc.listProperties(RDF.type);
        while (typeIt.hasNext()) {
            Statement stmt = typeIt.nextStatement();
            Resource type = stmt.getResource();
            if (validObjectTypes.contains(type)) {
                return type;
            }
        }

        return null;
    }

    private void validateFileObject(Resource resc, Resource parentResc) {
        if (!parentResc.hasProperty(RDF.type, Cdr.Work)) {
            failJob(null, "FileObject must be contained by Work, but FileObject {0}"
                    + " was contained by non-work parent {1}",
                    resc.getURI(), parentResc.getURI());
        }

        if (!resc.hasProperty(stagingLocation)) {
            failJob(null, "No staging location provided for file ({0})", resc.getURI());
        }
    }

    /**
     * @param aclValidator the aclValidator to set
     */
    public void setAclValidator(ContentObjectAccessRestrictionValidator aclValidator) {
        this.aclValidator = aclValidator;
    }
}
