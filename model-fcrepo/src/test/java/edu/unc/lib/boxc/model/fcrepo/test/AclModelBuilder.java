package edu.unc.lib.boxc.model.fcrepo.test;

import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;

import java.util.Calendar;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;

/**
 * Test utility for constructing models containing access control details using
 * a fluent api.
 *
 * @author bbpennel
 *
 */
public class AclModelBuilder {
    public Model model;
    private Resource resc;

    public AclModelBuilder(String title) {
        model = createDefaultModel();
        resc = model.getResource("");
        resc.addProperty(DC.title, title);
    }

    public AclModelBuilder(PID pid) {
        model = createDefaultModel();
        resc = model.getResource(pid.getRepositoryPath());
        resc.addProperty(DC.title, pid.getId());
    }

    public AclModelBuilder addUnitOwner(String princ) {
        return addProp(CdrAcl.unitOwner, princ);
    }

    public AclModelBuilder addCanManage(String princ) {
        return addProp(CdrAcl.canManage, princ);
    }

    public AclModelBuilder addCanIngest(String princ) {
        return addProp(CdrAcl.canIngest, princ);
    }

    public AclModelBuilder addCanAccess(String princ) {
        return addProp(CdrAcl.canAccess, princ);
    }

    public AclModelBuilder addCanViewMetadata(String princ) {
        return addProp(CdrAcl.canViewMetadata, princ);
    }

    public AclModelBuilder addCanViewAccessCopies(String princ) {
        return addProp(CdrAcl.canViewAccessCopies, princ);
    }

    public AclModelBuilder addCanViewOriginals(String princ) {
        return addProp(CdrAcl.canViewOriginals, princ);
    }

    public AclModelBuilder addNoneRole(String princ) {
        return addProp(CdrAcl.none, princ);
    }

    public AclModelBuilder addEmbargoUntil(Calendar date) {
        resc.addLiteral(CdrAcl.embargoUntil, date);
        return this;
    }

    public AclModelBuilder markForDeletion() {
        resc.addLiteral(CdrAcl.markedForDeletion, true);
        return this;
    }

    private AclModelBuilder addProp(Property prop, String princ) {
        resc.addProperty(prop, princ);
        return this;
    }
}
