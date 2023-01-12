package edu.unc.lib.boxc.auth.fcrepo.services;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Calendar;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.exceptions.InvalidAssignmentException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;

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

    @BeforeEach
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
        model.add(resc, CdrAcl.canViewOriginals, PUBLIC_PRINC);
        model.add(resc, CdrAcl.markedForDeletion, model.createTypedLiteral(false));

        validator.validate(resc);
    }

    @Test
    public void workInvalidPatronAccessTest() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            model.add(resc, RDF.type, Cdr.Work);
            model.add(resc, CdrAcl.canViewOriginals, "nobodynohow");

            try {
                validator.validate(resc);
            } catch (InvalidAssignmentException e) {
                assertTrue(e.getMessage().contains("Invalid staff principal"));
                throw e;
            }
        });
    }

    @Test
    public void workInvalidAclsTest() {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
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
        });
    }

    @Test
    public void validFolderTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Folder);
        model.add(resc, CdrAcl.embargoUntil, model.createTypedLiteral(Calendar.getInstance()));
        model.add(resc, CdrAcl.canViewMetadata, PUBLIC_PRINC);
        model.add(resc, CdrAcl.markedForDeletion, model.createTypedLiteral(true));

        validator.validate(resc);
    }

    @Test
    public void folderWithNoneRoleTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Folder);
        model.add(resc, CdrAcl.embargoUntil, model.createTypedLiteral(Calendar.getInstance()));
        model.add(resc, CdrAcl.none, PUBLIC_PRINC);

        validator.validate(resc);
    }

    @Test
    public void validFileObjectTest() throws Exception {
        model.add(resc, RDF.type, Cdr.FileObject);
        model.add(resc, CdrAcl.embargoUntil, model.createTypedLiteral(Calendar.getInstance()));
        model.add(resc, CdrAcl.canViewMetadata, AUTHENTICATED_PRINC);
        model.add(resc, CdrAcl.markedForDeletion, model.createTypedLiteral(true));

        validator.validate(resc);
    }

    @Test
    public void validCollectionTest() throws Exception {
        model.add(resc, RDF.type, Cdr.Collection);
        model.add(resc, CdrAcl.embargoUntil, model.createTypedLiteral(Calendar.getInstance()));
        model.add(resc, CdrAcl.canViewAccessCopies, PUBLIC_PRINC);
        model.add(resc, CdrAcl.canViewOriginals, AUTHENTICATED_PRINC);
        model.add(resc, CdrAcl.canIngest, STAFF_PRINC);
        model.add(resc, CdrAcl.canProcess, "processor_grp");
        model.add(resc, CdrAcl.canManage, OWNER_PRINC);

        validator.validate(resc);
    }

    @Test
    public void invalidPatronPrincipalCollectionTest() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            model.add(resc, RDF.type, Cdr.Collection);
            model.add(resc, CdrAcl.canIngest, PUBLIC_PRINC);

            try {
                validator.validate(resc);
            } catch (InvalidAssignmentException e) {
                assertTrue(e.getMessage().contains("Invalid patron principal"));
                throw e;
            }
        });
    }

    @Test
    public void invalidStaffPrincipalCollectionTest() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            model.add(resc, RDF.type, Cdr.Collection);
            model.add(resc, CdrAcl.canViewMetadata, STAFF_PRINC);

            try {
                validator.validate(resc);
            } catch (InvalidAssignmentException e) {
                assertTrue(e.getMessage().contains("Invalid staff principal"));
                throw e;
            }
        });
    }

    @Test
    public void emptyPrincipalCollectionTest() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            model.add(resc, RDF.type, Cdr.Collection);
            model.add(resc, CdrAcl.canViewOriginals, "");

            try {
                validator.validate(resc);
            } catch (InvalidAssignmentException e) {
                assertTrue(e.getMessage().contains("Cannot assign empty principal to role"));
                throw e;
            }
        });
    }

    @Test
    public void assignedUnitOwnerCollectionTest() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            model.add(resc, RDF.type, Cdr.Collection);
            model.add(resc, CdrAcl.unitOwner, OWNER_PRINC);

            try {
                validator.validate(resc);
            } catch (InvalidAssignmentException e) {
                assertTrue(e.getMessage().contains("invalid acl properties"));
                throw e;
            }
        });
    }

    @Test
    public void validAdminUnitTest() throws Exception {
        model.add(resc, RDF.type, Cdr.AdminUnit);
        model.add(resc, CdrAcl.canIngest, STAFF_PRINC);
        model.add(resc, CdrAcl.unitOwner, OWNER_PRINC);

        validator.validate(resc);
    }

    @Test
    public void invalidAclAdminUnitTest() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            model.add(resc, RDF.type, Cdr.AdminUnit);
            model.add(resc, CdrAcl.embargoUntil, model.createTypedLiteral(Calendar.getInstance()));
            model.add(resc, CdrAcl.canViewOriginals, AUTHENTICATED_PRINC);
            model.add(resc, CdrAcl.canViewMetadata, OWNER_PRINC);

            try {
                validator.validate(resc);
            } catch (InvalidAssignmentException e) {
                assertTrue(e.getMessage().contains("invalid acl properties"));
                assertTrue(e.getMessage().contains(CdrAcl.embargoUntil.getLocalName()));
                assertTrue(e.getMessage().contains(CdrAcl.canViewOriginals.getLocalName()));
                assertTrue(e.getMessage().contains(CdrAcl.canViewMetadata.getLocalName()));

                throw e;
            }
        });
    }

    @Test
    public void invalidPatronPrincipalAdminUnitTest() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            model.add(resc, RDF.type, Cdr.AdminUnit);
            model.add(resc, CdrAcl.canIngest, PUBLIC_PRINC);

            try {
                validator.validate(resc);
            } catch (InvalidAssignmentException e) {
                assertTrue(e.getMessage().contains("Invalid patron principal"));
                throw e;
            }
        });
    }

    @Test
    public void emptyPrincipalAdminUnitTest() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            model.add(resc, RDF.type, Cdr.AdminUnit);
            model.add(resc, CdrAcl.canDescribe, "");

            try {
                validator.validate(resc);
            } catch (InvalidAssignmentException e) {
                assertTrue(e.getMessage().contains("Cannot assign empty principal to role"));
                throw e;
            }
        });
    }

    @Test
    public void invalidObjectTypeTest() {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            model.add(resc, RDF.type, Cdr.ContentRoot);

            try {
                validator.validate(resc);
            } catch (InvalidAssignmentException e) {
                assertTrue(e.getMessage().contains("not applicable for access restrictions"));
                throw e;
            }
        });
    }

    @Test
    public void duplicatePrincipalUnit() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            model.add(resc, RDF.type, Cdr.AdminUnit);
            model.add(resc, CdrAcl.canManage, STAFF_PRINC);
            model.add(resc, CdrAcl.canIngest, STAFF_PRINC);

            validator.validate(resc);
        });
    }

    @Test
    public void duplicatePrincipalCollection() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            model.add(resc, RDF.type, Cdr.AdminUnit);
            model.add(resc, CdrAcl.canManage, STAFF_PRINC);
            model.add(resc, CdrAcl.canIngest, STAFF_PRINC);

            validator.validate(resc);
        });
    }

    @Test
    public void duplicatePrincipalContent() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            model.add(resc, RDF.type, Cdr.AdminUnit);
            model.add(resc, CdrAcl.canViewAccessCopies, PUBLIC_PRINC);
            model.add(resc, CdrAcl.none, PUBLIC_PRINC);

            validator.validate(resc);
        });
    }
}
