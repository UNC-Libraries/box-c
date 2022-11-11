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

import static edu.unc.lib.boxc.deposit.work.DepositGraphUtils.getChildIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.fcrepo.services.ContentObjectAccessRestrictionValidator;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

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

    private RepositoryObjectLoader repoObjLoader;

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

        cdrSchema = RDFDataMgr.loadModel("cdr-schema.ttl");

        reasoner = ReasonerRegistry.getOWLMicroReasoner().bindSchema(cdrSchema);
    }

    @Override
    public void runJob() {

        Model model = getReadOnlyModel();
        Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());

        // Validate all objects in the deposit model against the schema
        validateSchema(model);

        validatePrimaryObjects(model);

        // Retrieve the destination object so we can verify the model against where the deposit will land
        Map<String, String> depositStatus = getDepositStatus();
        String destinationPath = depositStatus.get(DepositField.containerId.name());
        PID destPid = PIDs.get(destinationPath);
        RepositoryObject parentObject = repoObjLoader.getRepositoryObject(destPid);

        // Validate each object in this deposit for ACL issues and others not covered by the schema
        // Start with the destination object as the parent, iterating through the new resources in the bag
        validateChildren(parentObject.getResource(), getChildIterator(depositBag));

        // Validate member order property
        validateMemberOrder(model);
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

    private void validateChildren(Resource parentResc, NodeIterator childIterator) {
        if (childIterator == null) {
            return;
        }

        if (parentResc.hasProperty(RDF.type, Cdr.FileObject)) {
            failJob(null, "FileObject {0} cannot contain children, but was provided as a container",
                    parentResc.getURI());
        }

        try {
            while (childIterator.hasNext()) {
                Resource childResc = (Resource) childIterator.next();

                Resource objType = getValidObjectTypeResc(childResc);
                if (objType == null) {
                    failJob(null, "Could not determine the type of object {0}", childResc.getURI());
                }

                if (childResc.hasProperty(RDF.type, Cdr.FileObject)) {
                    validateFileObject(childResc, parentResc);
                }

                // Verify that ACL assignments for this object are okay
                aclValidator.validate(childResc);

                NodeIterator iterator = getChildIterator(childResc);
                // Validate any children of this resource
                validateChildren(childResc, iterator);
            }
        } finally {
            childIterator.close();
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

        Resource origResc = DepositModelHelpers.getDatastream(resc);
        if (origResc == null) {
            failJob(null, "No original datastream for file object {0}", resc.getURI());
        } else if (!origResc.hasProperty(CdrDeposit.stagingLocation)) {
            failJob(null, "No staging location provided for file ({0})", resc.getURI());
        }
    }

    private void validateMemberOrder(Model model) {
        StmtIterator iterator = model.listStatements((Resource) null, Cdr.memberOrder, (RDFNode) null);

        while (iterator.hasNext()) {
            Statement statement = iterator.next();

            Resource subj = statement.getSubject();
            if (!subj.hasProperty(RDF.type, Cdr.Work)) {
                failJob(null, "Invalid property on {0}, only cdr:Work objects may specify member order.",
                        subj.getURI());
            }
            var orderValidator = new DepositSetMemberOrderValidator();
            orderValidator.setResource(subj);
            if (!orderValidator.isValid()) {
                failJob(null, "Member order is invalid for {0}, with the following errors: {1}",
                        subj.getURI(), orderValidator.getErrors());
            }
        }
    }

    /**
     * @param aclValidator the aclValidator to set
     */
    public void setAclValidator(ContentObjectAccessRestrictionValidator aclValidator) {
        this.aclValidator = aclValidator;
    }

    /**
     * @param repoObjLoader the repoObjLoader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }
}
