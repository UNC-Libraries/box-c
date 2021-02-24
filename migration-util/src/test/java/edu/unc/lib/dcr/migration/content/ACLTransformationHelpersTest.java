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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.DEPOSIT_RECORD_BASE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.jgroups.util.UUID;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.Bxc3UserRole;
import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.CDRProperty;
import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel;
import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.acl.util.AccessPrincipalConstants;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrAcl;

/**
 * @author bbpennel
 */
public class ACLTransformationHelpersTest {

    private final static String EMBARGO_END_DATE = "2040-01-01T00:00:00.000Z";
    private final static String BAD_EMBARGO_END_DATE = "2040-01-01T00:00:00";

    private PID pid;
    private PID parentPid;

    @Before
    public void setup() {
        pid = PIDs.get(UUID.randomUUID().toString());
        parentPid = PIDs.get(UUID.randomUUID().toString());
    }

    @Test
    public void transformPatronAccess_BothAssignedRole() throws Exception {
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.CONTAINER);

        addRoleForPublic(bxc3Resc, Bxc3UserRole.metadataPatron);
        addRoleForAuthenticated(bxc3Resc, Bxc3UserRole.patron);

        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Folder);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        assertEveryoneHasRole(CdrAcl.canViewMetadata, bxc5Resc);
        assertAuthenticatedHasRole(CdrAcl.canViewOriginals, bxc5Resc);
    }

    @Test
    public void transformPatronAccess_MultipleRolesForEveryone() throws Exception {
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.CONTAINER);

        addRoleForPublic(bxc3Resc, Bxc3UserRole.accessCopiesPatron);
        addRoleForPublic(bxc3Resc, Bxc3UserRole.metadataPatron);
        addRoleForPublic(bxc3Resc, Bxc3UserRole.patron);

        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Folder);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        assertEveryoneHasRole(CdrAcl.canViewMetadata, bxc5Resc);
    }

    @Test
    public void transformPatronAccess_NoRoles() throws Exception {
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.CONTAINER);

        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Folder);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        assertNoRolesAssignedforEveryone(bxc5Resc);
        assertNoRolesAssignedforAuthenticated(bxc5Resc);
    }

    @Test
    public void transformPatronAccess_NoRoles_InheritFalse() throws Exception {
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.CONTAINER);

        setInheritFromParent(bxc3Resc, false);

        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Folder);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        assertEveryoneHasRole(CdrAcl.none, bxc5Resc);
        assertAuthenticatedHasRole(CdrAcl.none, bxc5Resc);
    }

    @Test
    public void transformPatronAccess_OnlyPublicRole_InheritFalse() throws Exception {
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.CONTAINER);

        addRoleForPublic(bxc3Resc, Bxc3UserRole.accessCopiesPatron);
        setInheritFromParent(bxc3Resc, false);

        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Folder);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        assertEveryoneHasRole(CdrAcl.canViewAccessCopies, bxc5Resc);
        assertAuthenticatedHasRole(CdrAcl.canViewAccessCopies, bxc5Resc);
    }

    @Test
    public void transformPatronAccess_OnlyPublicRole_InheritTrue() throws Exception {
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.CONTAINER);

        addRoleForPublic(bxc3Resc, Bxc3UserRole.accessCopiesPatron);
        setInheritFromParent(bxc3Resc, true);

        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Folder);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        assertEveryoneHasRole(CdrAcl.canViewAccessCopies, bxc5Resc);
        assertNoRolesAssignedforAuthenticated(bxc5Resc);
    }


    @Test
    public void transformPatronAccess_EveryoneRole_WithEmbargo() throws Exception {
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.CONTAINER);

        addRoleForPublic(bxc3Resc, Bxc3UserRole.accessCopiesPatron);
        addEmbargo(bxc3Resc, EMBARGO_END_DATE);

        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Folder);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        assertEveryoneHasRole(CdrAcl.canViewAccessCopies, bxc5Resc);
        assertHasEmbargo(EMBARGO_END_DATE, bxc5Resc);
    }

    @Test
    public void transformPatronAccess_EveryoneRole_WithEmbargoInvalidBxc5Date() throws Exception {
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.CONTAINER);

        addRoleForPublic(bxc3Resc, Bxc3UserRole.accessCopiesPatron);
        addEmbargo(bxc3Resc, BAD_EMBARGO_END_DATE);

        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Folder);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        assertEveryoneHasRole(CdrAcl.canViewAccessCopies, bxc5Resc);
        assertHasEmbargo(EMBARGO_END_DATE, bxc5Resc);
    }

    @Test
    public void transformPatronAccess_Unpublished() throws Exception {
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.CONTAINER);

        addRoleForPublic(bxc3Resc, Bxc3UserRole.accessCopiesPatron);
        addRoleForAuthenticated(bxc3Resc, Bxc3UserRole.patron);
        setPublicationStatus(bxc3Resc, false);

        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Folder);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        assertEveryoneHasRole(CdrAcl.none, bxc5Resc);
        assertAuthenticatedHasRole(CdrAcl.none, bxc5Resc);
    }

    @Test
    public void transformPatronAccess_Published() throws Exception {
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.CONTAINER);

        addRoleForPublic(bxc3Resc, Bxc3UserRole.accessCopiesPatron);
        addRoleForAuthenticated(bxc3Resc, Bxc3UserRole.patron);
        setPublicationStatus(bxc3Resc, true);

        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Folder);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        assertEveryoneHasRole(CdrAcl.canViewAccessCopies, bxc5Resc);
        assertAuthenticatedHasRole(CdrAcl.canViewOriginals, bxc5Resc);
    }

    @Test
    public void transformPatronAccess_DisallowIndexing() throws Exception {
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.CONTAINER);

        addRoleForPublic(bxc3Resc, Bxc3UserRole.accessCopiesPatron);
        addRoleForAuthenticated(bxc3Resc, Bxc3UserRole.patron);
        setAllowIndexing(bxc3Resc, false);

        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Folder);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        assertEveryoneHasRole(CdrAcl.none, bxc5Resc);
        assertAuthenticatedHasRole(CdrAcl.none, bxc5Resc);
    }

    @Test
    public void transformPatronAccess_CollectionInUnit_BothWithEmbargoAndRoles() throws Exception {
        PID depositPid = PIDs.get(DEPOSIT_RECORD_BASE, UUID.randomUUID().toString());

        // First try to transform the unit
        Resource unitBxc3Resc = buildBoxc3Resource(parentPid, ContentModel.COLLECTION);

        addRoleForPublic(unitBxc3Resc, Bxc3UserRole.metadataPatron);
        addRoleForAuthenticated(unitBxc3Resc, Bxc3UserRole.accessCopiesPatron);
        addEmbargo(unitBxc3Resc, EMBARGO_END_DATE);

        Resource unitBxc5Resc = buildBoxc5Resource(parentPid, Cdr.AdminUnit);

        ACLTransformationHelpers.transformPatronAccess(unitBxc3Resc, unitBxc5Resc, depositPid);

        // Unit must not have any patron ACLs set
        assertNoRolesAssignedforEveryone(unitBxc5Resc);
        assertNoRolesAssignedforAuthenticated(unitBxc5Resc);
        assertFalse(unitBxc5Resc.hasProperty(CdrAcl.embargoUntil));

        // now transform the child collection, which has its own ACLs
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.COLLECTION);

        addRoleForPublic(bxc3Resc, Bxc3UserRole.accessCopiesPatron);
        addEmbargo(bxc3Resc, "2045-01-01T00:00:00");

        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Collection);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        // Only collection ACLs should be present
        assertEveryoneHasRole(CdrAcl.canViewAccessCopies, bxc5Resc);
        assertNoRolesAssignedforAuthenticated(bxc5Resc);
        assertHasEmbargo("2045-01-01T00:00:00.000Z", bxc5Resc);
    }

    @Test
    public void transformPatronAccess_Collection_NoAssignments_SetsDefaults() throws Exception {
        PID depositPid = PIDs.get(DEPOSIT_RECORD_BASE, UUID.randomUUID().toString());

        // First transform the unit
        Resource unitBxc3Resc = buildBoxc3Resource(parentPid, ContentModel.COLLECTION);
        Resource unitBxc5Resc = buildBoxc5Resource(parentPid, Cdr.AdminUnit);
        ACLTransformationHelpers.transformPatronAccess(unitBxc3Resc, unitBxc5Resc, depositPid);

        // now transform the child collection, which has its own ACLs
        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.COLLECTION);
        Resource bxc5Resc = buildBoxc5Resource(pid, Cdr.Collection);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        // Default collection assignments
        assertEveryoneHasRole(CdrAcl.canViewOriginals, bxc5Resc);
        assertAuthenticatedHasRole(CdrAcl.canViewOriginals, bxc5Resc);
    }

    @Test
    public void transformPatronAccess_DifferentBxc5Pid() throws Exception {
        PID bxc5Pid = PIDs.get(UUID.randomUUID().toString());

        Resource bxc3Resc = buildBoxc3Resource(pid, ContentModel.CONTAINER);

        addRoleForPublic(bxc3Resc, Bxc3UserRole.metadataPatron);
        addRoleForAuthenticated(bxc3Resc, Bxc3UserRole.patron);
        addEmbargo(bxc3Resc, EMBARGO_END_DATE);

        Resource bxc5Resc = buildBoxc5Resource(bxc5Pid, Cdr.Folder);

        ACLTransformationHelpers.transformPatronAccess(bxc3Resc, bxc5Resc, parentPid);

        assertEveryoneHasRole(CdrAcl.canViewMetadata, bxc5Resc);
        assertAuthenticatedHasRole(CdrAcl.canViewOriginals, bxc5Resc);
        assertHasEmbargo(EMBARGO_END_DATE, bxc5Resc);
    }

    private Resource buildBoxc3Resource(PID pid, ContentModel contentModel) {
        Model bxc3Model = ModelFactory.createDefaultModel();
        Resource bxc3Resc = bxc3Model.getResource(pid.getRepositoryPath());
        bxc3Resc.addProperty(FedoraProperty.hasModel.getProperty(), contentModel.getResource());

        return bxc3Resc;
    }

    private void addRoleForPublic(Resource bxc3Resc, Bxc3UserRole role) {
        bxc3Resc.addLiteral(role.getProperty(), ACLTransformationHelpers.BXC3_PUBLIC_GROUP);
    }

    private void addRoleForAuthenticated(Resource bxc3Resc, Bxc3UserRole role) {
        bxc3Resc.addLiteral(role.getProperty(), ACLTransformationHelpers.BXC3_AUTHENTICATED_GROUP);
    }

    private void setInheritFromParent(Resource bxc3Resc, boolean inherit) {
        bxc3Resc.addLiteral(CDRProperty.inheritPermissions.getProperty(), Boolean.toString(inherit));
    }

    private void addEmbargo(Resource bxc3Resc, String embargoEndDate) {
        Literal embargoLiteral = ResourceFactory.createTypedLiteral(embargoEndDate, XSDDatatype.XSDdateTime);
        bxc3Resc.addLiteral(CDRProperty.embargoUntil.getProperty(), embargoLiteral);
    }

    private void setPublicationStatus(Resource bxc3Resc, boolean isPublished) {
        bxc3Resc.addLiteral(CDRProperty.isPublished.getProperty(), isPublished ? "yes" : "no");
    }

    private void setAllowIndexing(Resource bxc3Resc, boolean allow) {
        bxc3Resc.addLiteral(CDRProperty.allowIndexing.getProperty(), allow ? "yes" : "no");
    }

    private Resource buildBoxc5Resource(PID pid, Resource resourceType) {
        Model bxc5Model = ModelFactory.createDefaultModel();
        Resource bxc5Resc = bxc5Model.getResource(pid.getRepositoryPath());
        bxc5Resc.addProperty(RDF.type, resourceType);

        return bxc5Resc;
    }

    private void assertEveryoneHasRole(Property role, Resource bxc5Resc) {
        assertTrue("Everyone group did not have the expected role " + role + " for resource " + bxc5Resc.getURI(),
                bxc5Resc.hasLiteral(role, AccessPrincipalConstants.PUBLIC_PRINC));
    }

    private void assertAuthenticatedHasRole(Property role, Resource bxc5Resc) {
        assertTrue("Authenticated group did not have the expected role " + role + " for resource " + bxc5Resc.getURI(),
                bxc5Resc.hasLiteral(role, AccessPrincipalConstants.AUTHENTICATED_PRINC));
    }

    private void assertNoRolesAssignedforEveryone(Resource bxc5Resc) {
        List<Statement> roles = bxc5Resc.getModel().listStatements(
                bxc5Resc, null, AccessPrincipalConstants.PUBLIC_PRINC).toList();
        assertTrue("Expected no roles for everyone group on " + bxc5Resc.getURI(), roles.isEmpty());
    }

    private void assertNoRolesAssignedforAuthenticated(Resource bxc5Resc) {
        List<Statement> roles = bxc5Resc.getModel().listStatements(
                bxc5Resc, null, AccessPrincipalConstants.AUTHENTICATED_PRINC).toList();
        assertTrue("Expected no roles for authenticated group on " + bxc5Resc.getURI(), roles.isEmpty());
    }

    private void assertHasEmbargo(String embargoEndDate, Resource bxc5Resc) {
        assertTrue("Resource " + bxc5Resc.getURI() + " did not have expected embargo with end date " + embargoEndDate,
                bxc5Resc.hasLiteral(CdrAcl.embargoUntil,
                        ResourceFactory.createTypedLiteral(embargoEndDate, XSDDatatype.XSDdateTime).getValue()));
    }
}
