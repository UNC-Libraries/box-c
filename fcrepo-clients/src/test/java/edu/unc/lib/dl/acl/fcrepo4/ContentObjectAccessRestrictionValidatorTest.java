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

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Calendar;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.exception.InvalidAssignmentException;
import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrAcl;

/**
 *
 * @author bbpennel
 *
 */
public class ContentObjectAccessRestrictionValidatorTest {

    private static final String STAFF_PRINC = "staff_princ";
    private static final String OWNER_PRINC = "owner_princ";

    private static final String PID_URI =
            "http://example.com/rest/content/99/9a/01/a2/999a01a2-c836-499a-a72e-57f1039f4f45";

    private ContentObjectAccessRestrictionValidator validator;

    @Mock
    private PID pid;

    private Model model;
    private Resource resc;

    @Before
    public void init() {
        initMocks(this);

        validator = new ContentObjectAccessRestrictionValidator();
        when(pid.getURI()).thenReturn(PID_URI);

        model = ModelFactory.createDefaultModel();
        resc = model.getResource(PID_URI);
    }

    @Test
    public void workNoAclsTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Work);

        validator.validate(resc);
    }

    @Test
    public void validWorkTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Work);
        model.add(resc, CdrAcl.embargoUntil, model.createTypedLiteral(Calendar.getInstance()));
        model.add(resc, CdrAcl.patronAccess, PatronAccess.none.name());
        model.add(resc, CdrAcl.markedForDeletion, model.createTypedLiteral(false));

        validator.validate(resc);
    }

    @Test(expected = InvalidAssignmentException.class)
    public void workInvalidPatronAccessTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Work);
        model.add(resc, CdrAcl.patronAccess, "nobodynohow");

        try {
            validator.validate(resc);
        } catch (InvalidAssignmentException e) {
            assertTrue(e.getMessage().contains("patron access"));
            throw e;
        }
    }

    @Test(expected = InvalidAssignmentException.class)
    public void workInvalidAclsTest() {
        model.add(resc, RDF.type, Cdr.Work);
        model.add(resc, CdrAcl.canAccess, STAFF_PRINC);
        model.add(resc, CdrAcl.unitOwner, OWNER_PRINC);

        try {
            validator.validate(resc);
        } catch (InvalidAssignmentException e) {
            assertTrue(e.getMessage().contains("invalid acl properties"));
            assertTrue(e.getMessage().contains(CdrAcl.canAccess.getLocalName()));
            assertTrue(e.getMessage().contains(CdrAcl.unitOwner.getLocalName()));

            throw e;
        }
    }

    @Test
    public void validFolderTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Folder);
        model.add(resc, CdrAcl.embargoUntil, model.createTypedLiteral(Calendar.getInstance()));
        model.add(resc, CdrAcl.patronAccess, PatronAccess.parent.name());
        model.add(resc, CdrAcl.markedForDeletion, model.createTypedLiteral(true));

        validator.validate(resc);
    }

    @Test
    public void validFileObjectTest() throws Exception {
        model.add(resc, RDF.type, Cdr.FileObject);
        model.add(resc, CdrAcl.embargoUntil, model.createTypedLiteral(Calendar.getInstance()));
        model.add(resc, CdrAcl.patronAccess, PatronAccess.authenticated.name());
        model.add(resc, CdrAcl.markedForDeletion, model.createTypedLiteral(true));

        validator.validate(resc);
    }

    @Test
    public void validCollectionTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Collection);
        model.add(resc, CdrAcl.embargoUntil, model.createTypedLiteral(Calendar.getInstance()));
        model.add(resc, CdrAcl.canViewOriginals, AUTHENTICATED_PRINC);
        model.add(resc, CdrAcl.canIngest, STAFF_PRINC);
        model.add(resc, CdrAcl.canManage, OWNER_PRINC);

        validator.validate(resc);
    }

    @Test(expected = InvalidAssignmentException.class)
    public void invalidAclCollectionTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Collection);
        model.add(resc, CdrAcl.patronAccess, PatronAccess.parent.name());

        try {
            validator.validate(resc);
        } catch (InvalidAssignmentException e) {
            assertTrue(e.getMessage().contains("invalid acl properties"));
            assertTrue(e.getMessage().contains(CdrAcl.patronAccess.getLocalName()));

            throw e;
        }
    }

    @Test(expected = InvalidAssignmentException.class)
    public void tooManyPatronsCollectionTest() {
        model.add(resc, RDF.type, Cdr.Collection);
        model.add(resc, CdrAcl.canViewOriginals, AUTHENTICATED_PRINC);
        model.add(resc, CdrAcl.canViewMetadata, PUBLIC_PRINC);


        try {
            validator.validate(resc);
        } catch (InvalidAssignmentException e) {
            assertTrue(e.getMessage().contains("Too many patron roles assigned"));
            throw e;
        }
    }

    @Test(expected = InvalidAssignmentException.class)
    public void invalidPatronPrincipalCollectionTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Collection);
        model.add(resc, CdrAcl.canIngest, PUBLIC_PRINC);

        try {
            validator.validate(resc);
        } catch (InvalidAssignmentException e) {
            assertTrue(e.getMessage().contains("Invalid patron principal"));
            throw e;
        }
    }

    @Test(expected = InvalidAssignmentException.class)
    public void invalidStaffPrincipalCollectionTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Collection);
        model.add(resc, CdrAcl.canViewMetadata, STAFF_PRINC);

        try {
            validator.validate(resc);
        } catch (InvalidAssignmentException e) {
            assertTrue(e.getMessage().contains("Invalid staff principal"));
            throw e;
        }
    }

    @Test(expected = InvalidAssignmentException.class)
    public void emptyPrincipalCollectionTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Collection);
        model.add(resc, CdrAcl.canViewOriginals, "");

        try {
            validator.validate(resc);
        } catch (InvalidAssignmentException e) {
            assertTrue(e.getMessage().contains("Cannot assign empty principal to role"));
            throw e;
        }
    }

    @Test(expected = InvalidAssignmentException.class)
    public void assignedUnitOwnerCollectionTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Collection);
        model.add(resc, CdrAcl.unitOwner, OWNER_PRINC);

        try {
            validator.validate(resc);
        } catch (InvalidAssignmentException e) {
            assertTrue(e.getMessage().contains("invalid acl properties"));
            throw e;
        }
    }

    @Test
    public void validAdminUnitTest() throws Exception {
        model.add(resc, RDF.type, Cdr.AdminUnit);
        model.add(resc, CdrAcl.canIngest, STAFF_PRINC);
        model.add(resc, CdrAcl.unitOwner, OWNER_PRINC);

        validator.validate(resc);
    }

    @Test(expected = InvalidAssignmentException.class)
    public void invalidAclAdminUnitTest() throws Exception {
        model.add(resc, RDF.type, Cdr.AdminUnit);
        model.add(resc, CdrAcl.embargoUntil, model.createTypedLiteral(Calendar.getInstance()));
        model.add(resc, CdrAcl.canViewOriginals, AUTHENTICATED_PRINC);
        model.add(resc, CdrAcl.patronAccess, OWNER_PRINC);

        try {
            validator.validate(resc);
        } catch (InvalidAssignmentException e) {
            assertTrue(e.getMessage().contains("invalid acl properties"));
            assertTrue(e.getMessage().contains(CdrAcl.embargoUntil.getLocalName()));
            assertTrue(e.getMessage().contains(CdrAcl.canViewOriginals.getLocalName()));
            assertTrue(e.getMessage().contains(CdrAcl.patronAccess.getLocalName()));

            throw e;
        }
    }

    @Test(expected = InvalidAssignmentException.class)
    public void invalidPatronPrincipalAdminUnitTest() throws Exception {
        model.add(resc, RDF.type, Cdr.AdminUnit);
        model.add(resc, CdrAcl.canIngest, PUBLIC_PRINC);

        try {
            validator.validate(resc);
        } catch (InvalidAssignmentException e) {
            assertTrue(e.getMessage().contains("Invalid patron principal"));
            throw e;
        }
    }

    @Test(expected = InvalidAssignmentException.class)
    public void emptyPrincipalAdminUnitTest() throws Exception {
        model.add(resc, RDF.type, Cdr.AdminUnit);
        model.add(resc, CdrAcl.canDescribe, "");

        try {
            validator.validate(resc);
        } catch (InvalidAssignmentException e) {
            assertTrue(e.getMessage().contains("Cannot assign empty principal to role"));
            throw e;
        }
    }

    @Test(expected = InvalidAssignmentException.class)
    public void invalidObjectTypeTest() {
        model.add(resc, RDF.type, Cdr.ContentRoot);

        try {
            validator.validate(resc);
        } catch (InvalidAssignmentException e) {
            assertTrue(e.getMessage().contains("not applicable for access restrictions"));
            throw e;
        }
    }
}
