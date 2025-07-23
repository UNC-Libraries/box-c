package edu.unc.lib.boxc.operations.impl.destroy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;

/**
 *
 * @author harring
 *
 */
public class TombstonePropertyPredicateTest {

    @Test
    public void testPermittedPropertyAnySubject() {
        TombstonePropertyPredicate predicate = new TombstonePropertyPredicate();
        Statement s = ResourceFactory.createStatement(ResourceFactory.createResource(), DcElements.title,
                ResourceFactory.createPlainLiteral("title"));
        assertTrue(predicate.test(s));
    }

    @Test
    public void testPermittedPropertySpecificSubject() {
        Resource resc = ResourceFactory.createResource();
        TombstonePropertyPredicate predicate = new TombstonePropertyPredicate(resc);
        Statement s1 = ResourceFactory.createStatement(resc, DcElements.title,
                ResourceFactory.createPlainLiteral("title"));
        assertTrue(predicate.test(s1));
        Resource unrelatedResc = ResourceFactory.createResource();
        Statement s2 = ResourceFactory.createStatement(unrelatedResc, DcElements.title,
                ResourceFactory.createPlainLiteral("title"));
        assertFalse(predicate.test(s2));
    }

    @Test
    public void testPropertyNotPermittedAnySubject() {
        TombstonePropertyPredicate predicate = new TombstonePropertyPredicate();
        Statement s = ResourceFactory.createStatement(ResourceFactory.createResource(), Ebucore.privateTelephoneNumber,
                ResourceFactory.createPlainLiteral("800-FOR-BOXY"));
        assertFalse(predicate.test(s));
    }

    @Test
    public void testPropertyNotPermittedSpecificSubject() {
        Resource resc = ResourceFactory.createResource();
        TombstonePropertyPredicate predicate = new TombstonePropertyPredicate(resc);
        Statement s = ResourceFactory.createStatement(resc, Ebucore.privateTelephoneNumber,
                ResourceFactory.createPlainLiteral("800-FOR-BOXY"));
        assertFalse(predicate.test(s));
    }

}
