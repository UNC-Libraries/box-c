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
package edu.unc.lib.dcr.migration.content;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.Bxc3UserRole;
import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrAcl;

/**
 * Helper methods for transforming content object access control settings
 * from bxc3 to box5 expectations
 *
 * @author bbpennel
 */
public class ACLTransformationHelpers {

    public final static String BXC3_PUBLIC_GROUP = "public";
    public final static String BXC3_AUTHENTICATED_GROUP = "authenticated";

    private static final Map<PID, Model> unitPatronAccessCache = new HashMap<>();

    /**
     * Transforms the bxc3 patron access control settings into bxc5
     *
     * Patron access settings found on an admin unit will be applied to its children
     * collections instead, but only if the children do not have locally defined
     * settings for the same groups.
     *
     * @param bxc3Resc
     * @param bxc5Resc
     * @param parentPid bxc5 PID of the parent
     */
    public static void transformPatronAccess(Resource bxc3Resc, Resource bxc5Resc, PID parentPid) {
        // For admin units, cache patron access settings so they can be used for children instead
        Resource destResc;
        if (bxc5Resc.hasProperty(RDF.type, Cdr.AdminUnit)) {
            Model unitModel = createDefaultModel();
            PID unitPid = PIDs.get(bxc5Resc.getURI());
            destResc = unitModel.getResource(unitPid.getRepositoryPath());
            unitPatronAccessCache.put(unitPid, unitModel);
        } else {
            destResc = bxc5Resc;
        }

        if (bxc3Resc.hasProperty(CDRProperty.embargoUntil.getProperty())) {
            destResc.addLiteral(CdrAcl.embargoUntil,
                    bxc3Resc.getProperty(CDRProperty.embargoUntil.getProperty()).getString());
        }

        Property[] patronRoles = calculatePatronRoles(bxc3Resc);
        Property everyoneRole = patronRoles[0];
        Property authRole = patronRoles[1];

        if (everyoneRole != null) {
            destResc.addLiteral(everyoneRole, PUBLIC_PRINC);
        }
        if (authRole != null) {
            destResc.addLiteral(authRole, AUTHENTICATED_PRINC);
        }

        // Merge in access settings from parent if present in the cache
        mergeParentPatronAcls(parentPid, destResc, everyoneRole, authRole);
    }

    private static Property[] calculatePatronRoles(Resource bxc3Resc) {
        if (bxc3Resc.hasLiteral(CDRProperty.isPublished.getProperty(), "no")
                || bxc3Resc.hasLiteral(CDRProperty.allowIndexing.getProperty(), "no")) {
            return new Property[] { CdrAcl.none, CdrAcl.none };
        }

        Property everyoneRole = null;
        Property authRole = null;

        StmtIterator stmtIt = bxc3Resc.listProperties();
        while (stmtIt.hasNext()) {
            Statement stmt = stmtIt.next();
            if (!stmt.getObject().isLiteral()) {
                continue;
            }

            String objectVal = stmt.getObject().asLiteral().getLexicalForm();
            if (BXC3_PUBLIC_GROUP.equals(objectVal)) {
                everyoneRole = mostRestrictiveRole(everyoneRole, stmt.getPredicate());
            } else if (BXC3_AUTHENTICATED_GROUP.equals(objectVal)) {
                authRole = mostRestrictiveRole(authRole, stmt.getPredicate());
            }
        }

        boolean inherit = true;
        if (bxc3Resc.hasProperty(CDRProperty.inheritPermissions.getProperty())) {
            Statement stmt = bxc3Resc.getProperty(CDRProperty.inheritPermissions.getProperty());
            inherit = Boolean.parseBoolean(stmt.getString());
        }

        if (!inherit) {
            if (everyoneRole == null) {
                everyoneRole = CdrAcl.none;
            }
            if (authRole == null) {
                authRole = everyoneRole;
            }
        }

        return new Property[] { everyoneRole, authRole };
    }

    private static Property mostRestrictiveRole(Property existingRole, Property bxc3Role) {
        // Role can't become more restrictive via role property
        if (CdrAcl.canViewMetadata.equals(existingRole) || CdrAcl.none.equals(existingRole)) {
            return existingRole;
        }

        if (!Bxc3UserRole.metadataPatron.equals(bxc3Role)
                && !Bxc3UserRole.accessCopiesPatron.equals(bxc3Role)
                && !Bxc3UserRole.patron.equals(bxc3Role)) {
            return existingRole;
        }

        if (Bxc3UserRole.metadataPatron.equals(bxc3Role)) {
            return CdrAcl.canViewMetadata;
        }
        if (Bxc3UserRole.accessCopiesPatron.equals(bxc3Role)) {
            return CdrAcl.canViewAccessCopies;
        }
        if (Bxc3UserRole.patron.equals(bxc3Role)) {
            if (CdrAcl.canViewAccessCopies.equals(existingRole)) {
                return existingRole;
            } else {
                return CdrAcl.canViewOriginals;
            }
        }

        return existingRole;
    }

    private static void mergeParentPatronAcls(PID parentPid, Resource destResc, Property everyoneRole, Property authRole) {
        // Merge in access settings from parent if present in the cache
        Model parentUnitModel = unitPatronAccessCache.get(parentPid);
        if (parentUnitModel != null) {
            Resource parentUnitResc = parentUnitModel.getResource(parentPid.getRepositoryPath());
            if (!destResc.hasProperty(CdrAcl.embargoUntil) && parentUnitResc.hasProperty(CdrAcl.embargoUntil)) {
                destResc.addLiteral(CdrAcl.embargoUntil,
                        parentUnitResc.getProperty(CdrAcl.embargoUntil).getLiteral().getString());
            }
            if (everyoneRole == null) {
                StmtIterator it = parentUnitModel.listStatements(parentUnitResc, null, PUBLIC_PRINC);
                if (it.hasNext()) {
                    Statement roleStmt = it.next();
                    destResc.addLiteral(roleStmt.getPredicate(), PUBLIC_PRINC);
                    it.close();
                }
            }
            if (authRole == null) {
                StmtIterator it = parentUnitModel.listStatements(parentUnitResc, null, AUTHENTICATED_PRINC);
                if (it.hasNext()) {
                    Statement roleStmt = it.next();
                    destResc.addLiteral(roleStmt.getPredicate(), AUTHENTICATED_PRINC);
                    it.close();
                }
            }
        }
    }
}
