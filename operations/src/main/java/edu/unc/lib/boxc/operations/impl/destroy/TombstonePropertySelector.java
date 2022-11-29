package edu.unc.lib.boxc.operations.impl.destroy;

import java.util.Arrays;
import java.util.List;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.rdf.Premis;

/**
 * Filters statements against a list of permitted predicates
 *
 * @author harring
 *
 */
public class TombstonePropertySelector extends SimpleSelector {

    static final List<Property> permittedPredicates = Arrays.asList(
            Cdr.hasMessageDigest, Cdr.hasSize, Cdr.hasEvents, DcElements.title,
            Ebucore.filename, Ebucore.hasMimeType, Premis.hasOriginalName,
            Premis.hasMessageDigest, Premis.hasSize, RDF.type, PcdmModels.memberOf);

    /**
     * Selects only those statements whose predicates match one of the permitted predicates
     */
    @Override
    public boolean selects(Statement s) {
        return (subject == null || s.getSubject().equals(subject))
            && (permittedPredicates.contains(s.getPredicate()));
    }

    public TombstonePropertySelector(Resource subject) {
        super(subject, (Property) null, (Object) null);
     }

    public TombstonePropertySelector() {
        super();
    }

}
