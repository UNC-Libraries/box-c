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
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;

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

    private static final Logger log = getLogger(ACLTransformationHelpers.class);

    public final static String BXC3_PUBLIC_GROUP = "public";
    public final static String BXC3_AUTHENTICATED_GROUP = "authenticated";

    private ACLTransformationHelpers() {
    }

    /**
     * Transforms the bxc3 patron access control settings into bxc5
     *
     * @param bxc3Resc
     * @param bxc5Resc
     * @param parentPid bxc5 PID of the parent
     */
    public static void transformPatronAccess(Resource bxc3Resc, Resource bxc5Resc, PID parentPid) {
        Resource destResc;
        if (bxc5Resc.hasProperty(RDF.type, Cdr.AdminUnit)) {
            // Ignoring permissions from unit
            Model unitModel = createDefaultModel();
            PID unitPid = PIDs.get(bxc5Resc.getURI());
            destResc = unitModel.getResource(unitPid.getRepositoryPath());
        } else {
            destResc = bxc5Resc;
        }

        // Migrate existing embargoes
        if (bxc3Resc.hasProperty(CDRProperty.embargoUntil.getProperty())) {
            String embargoDate = formatEmbargoDate(bxc3Resc.getProperty(
                    CDRProperty.embargoUntil.getProperty()).getString());
            Literal embargoLiteral = ResourceFactory.createTypedLiteral(embargoDate, XSDDatatype.XSDdateTime);
            destResc.addLiteral(CdrAcl.embargoUntil, embargoLiteral);
            ACLTransformationReport.hasEmbargo.incrementAndGet();
        }

        // Calculate the most restrictive roles assigned to each patron group
        Property[] patronRoles = calculatePatronRoles(bxc3Resc);
        Property everyoneRole = patronRoles[0];
        Property authRole = patronRoles[1];

        // assign the patron groups roles if they were specified
        if (everyoneRole != null) {
            destResc.addLiteral(everyoneRole, PUBLIC_PRINC);
        }
        if (authRole != null) {
            destResc.addLiteral(authRole, AUTHENTICATED_PRINC);
        }

        // For collections if no roles specified or inherited, default to open permissions
        // as they would normally inherit from the root in bxc3.
        if (bxc5Resc.hasProperty(RDF.type, Cdr.Collection)) {
            if (!(bxc5Resc.hasProperty(CdrAcl.canViewMetadata)
                    || bxc5Resc.hasProperty(CdrAcl.canViewAccessCopies)
                    || bxc5Resc.hasProperty(CdrAcl.canViewOriginals)
                    || bxc5Resc.hasProperty(CdrAcl.none))) {
                destResc.addLiteral(CdrAcl.canViewOriginals, PUBLIC_PRINC);
                destResc.addLiteral(CdrAcl.canViewOriginals, AUTHENTICATED_PRINC);
            }
        }
    }

    private static Property[] calculatePatronRoles(Resource bxc3Resc) {
        if (bxc3Resc.hasLiteral(CDRProperty.isPublished.getProperty(), "no")
                || bxc3Resc.hasLiteral(CDRProperty.allowIndexing.getProperty(), "no")) {
            ACLTransformationReport.isUnpublished.incrementAndGet();
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
            Property pred = stmt.getPredicate();
            if (BXC3_PUBLIC_GROUP.equals(objectVal)) {
                everyoneRole = mostRestrictiveRole(everyoneRole, pred);
            } else if (BXC3_AUTHENTICATED_GROUP.equals(objectVal)) {
                authRole = mostRestrictiveRole(authRole, pred);
            } else if (isPatronRole(pred)) {
                log.warn("Skipping invalid patron group {} assigned role {} on {}",
                        objectVal, pred.getLocalName(), bxc3Resc.getURI());
                ACLTransformationReport.hasInvalidPatronGroup.incrementAndGet();
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

    private static Set<String> BXC3_PATRON_ROLES = new HashSet<>(Arrays.asList(
            Bxc3UserRole.metadataPatron.getProperty().getURI(),
            Bxc3UserRole.accessCopiesPatron.getProperty().getURI(),
            Bxc3UserRole.patron.getProperty().getURI()));
    private static boolean isPatronRole(Property property) {
        return BXC3_PATRON_ROLES.contains(property.getURI());
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

    private static String formatEmbargoDate(String embargoDate) {
        String regex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$"; // ISO date without milliseconds
        Pattern pattern = Pattern.compile(regex);

        if (pattern.matcher(embargoDate.trim()).matches()) {
            embargoDate += ".000Z";
        }

        return embargoDate;
    }
}
