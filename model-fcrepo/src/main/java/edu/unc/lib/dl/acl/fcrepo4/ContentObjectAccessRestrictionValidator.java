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
package edu.unc.lib.dl.acl.fcrepo4;

import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.canAccess;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.canDescribe;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.canIngest;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.canManage;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.canProcess;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.canViewAccessCopies;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.canViewMetadata;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.canViewOriginals;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.embargoUntil;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.markedForDeletion;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.none;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.unitOwner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.dl.acl.exception.InvalidAssignmentException;
import edu.unc.lib.dl.acl.util.PrincipalClassifier;
import edu.unc.lib.dl.acl.util.UserRole;

/**
 * Validator which determines if a provided model contains access restriction
 * properties which are valid for the type of content object described.
 *
 * @author bbpennel
 *
 */
public class ContentObjectAccessRestrictionValidator {

    private static final Set<Property> collectionProperties = new HashSet<>(Arrays.asList(
            canViewMetadata,
            canViewAccessCopies,
            canViewOriginals,
            canAccess,
            canDescribe,
            canIngest,
            canProcess,
            canManage,
            none,
            embargoUntil,
            markedForDeletion));

    private static final Set<Property> adminUnitProperties = new HashSet<>(Arrays.asList(
            canAccess,
            canDescribe,
            canIngest,
            canProcess,
            canManage,
            unitOwner,
            markedForDeletion));

    private static final Set<Property> contentProperties = new HashSet<>(Arrays.asList(
            canViewMetadata,
            canViewAccessCopies,
            canViewOriginals,
            none,
            embargoUntil,
            markedForDeletion));

    private final Set<Resource> validObjectTypes;

    public ContentObjectAccessRestrictionValidator() {
        validObjectTypes = new HashSet<>(Arrays.asList(
                Cdr.FileObject, Cdr.Work, Cdr.Folder, Cdr.Collection, Cdr.AdminUnit));
    }

    /**
     * Validate the provided model, throwing an InvalidAssignmentException if
     * any of the assigned restrictions are invalid for the type of object
     * provided.
     *
     * @param resc model containing properties of the object being validated
     * @throws InvalidAssignmentException
     */
    public void validate(Resource resc) throws InvalidAssignmentException {

        Resource objType = getValidObjectTypeResc(resc);

        if (objType == null) {
            throw new InvalidAssignmentException(
                    "Object " + resc.getURI() + " is not applicable for access restrictions.");
        }

        if (objType.equals(Cdr.FileObject)
                || objType.equals(Cdr.Work)
                || objType.equals(Cdr.Folder)) {
            assertApplicableProperties(resc, objType, contentProperties);
            // verify that principals are syntactically valid
            assertValidPrincipals(resc);
        } else if (objType.equals(Cdr.Collection)) {
            assertApplicableProperties(resc, Cdr.Collection, collectionProperties);
            // verify that principals are syntactically valid
            assertValidPrincipals(resc);
        } else {
            assertApplicableProperties(resc, Cdr.AdminUnit, adminUnitProperties);
            // verify that principals are syntactically valid
            assertValidPrincipals(resc);
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

    /**
     * Asserts that for any role assignments being made, the principal assigned
     * is a valid value for that role, depending on if it is a staff or patron
     * role.
     *
     * @param resc
     * @throws InvalidAssignmentException
     */
    private void assertValidPrincipals(Resource resc) throws InvalidAssignmentException {
        Set<String> existingPrincipals = new HashSet<>();

        StmtIterator it = resc.listProperties();
        while (it.hasNext()) {
            Statement stmt = it.nextStatement();

            // Skip non-acl properties
            String ns = stmt.getPredicate().getNameSpace();
            if (!ns.equals(CdrAcl.NS)) {
                continue;
            }

            String predUri = stmt.getPredicate().getURI();
            UserRole role = UserRole.getRoleByProperty(predUri);
            if (role == null) {
                continue;
            }

            String princ = stmt.getString();

            if (StringUtils.isEmpty(princ)) {
                throw new InvalidAssignmentException("Cannot assign empty principal to role "
                        + predUri + " for object " + resc.getURI());
            }
            if (existingPrincipals.contains(princ)) {
                throw new InvalidAssignmentException("Cannot assign multiple roles to the same principal "
                        + princ + " for object " + resc.getURI());
            }

            boolean isPatronReservedPrinc = PrincipalClassifier.isPatronPrincipal(princ);

            if (role.isStaffRole() && isPatronReservedPrinc) {
                throw new InvalidAssignmentException("Invalid patron principal '" + princ
                        + "' assigned to staff role " + predUri + " for object " + resc.getURI());
            }
            if (role.isPatronRole() && !isPatronReservedPrinc) {
                throw new InvalidAssignmentException("Invalid staff principal '" + princ
                        + "' assigned to patron role " + predUri + " for object " + resc.getURI());
            }

            existingPrincipals.add(princ);
        }
    }

    /**
     * Assert that the resource given does not contain any access restriction
     * assignments that are invalid for its object type
     *
     * @param resc
     *            resource for the object being validated
     * @param objType
     *            RDF type of the object being validated
     * @param allowed
     *            list of properties which are allowed for this object type
     * @throws InvalidAssignmentException
     *             Thrown if any invalid access restrictions are assigned.
     */
    private void assertApplicableProperties(Resource resc, Resource objType, Set<Property> allowed)
            throws InvalidAssignmentException {
        List<String> invalidProperties = new ArrayList<>();

        StmtIterator it = resc.listProperties();
        while (it.hasNext()) {
            Statement stmt = it.nextStatement();
            String ns = stmt.getPredicate().getNameSpace();
            if (!ns.equals(CdrAcl.NS)) {
                continue;
            }
            if (!allowed.contains(stmt.getPredicate())) {
                invalidProperties.add(stmt.getPredicate().getURI());
            }
        }

        if (!invalidProperties.isEmpty()) {
            String msg = String.format("Resource %s of type %s contained invalid acl properties:%n %s",
                    resc.getURI(), objType.getURI(), String.join("\n", invalidProperties));
            throw new InvalidAssignmentException(msg);
        }

    }
}
