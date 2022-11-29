package edu.unc.lib.boxc.operations.impl.destroy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.junit.Test;

import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.operations.impl.destroy.TombstonePropertySelector;

/**
 *
 * @author harring
 *
 */
public class TombstonePropertySelectorTest {

    @Test
    public void testPermittedPropertyAnySubject() {
        TombstonePropertySelector selector = new TombstonePropertySelector();
        Statement s = ResourceFactory.createStatement(ResourceFactory.createResource(), DcElements.title,
                ResourceFactory.createPlainLiteral("title"));
        assertTrue(selector.selects(s));
    }

    @Test
    public void testPermittedPropertySpecificSubject() {
        Resource resc = ResourceFactory.createResource();
        TombstonePropertySelector selector = new TombstonePropertySelector(resc);
        Statement s1 = ResourceFactory.createStatement(resc, DcElements.title,
                ResourceFactory.createPlainLiteral("title"));
        assertTrue(selector.selects(s1));
        Resource unrelatedResc = ResourceFactory.createResource();
        Statement s2 = ResourceFactory.createStatement(unrelatedResc, DcElements.title,
                ResourceFactory.createPlainLiteral("title"));
        assertFalse(selector.selects(s2));
    }

    @Test
    public void testPropertyNotPermittedAnySubject() {
        TombstonePropertySelector selector = new TombstonePropertySelector();
        Statement s = ResourceFactory.createStatement(ResourceFactory.createResource(), Ebucore.privateTelephoneNumber,
                ResourceFactory.createPlainLiteral("800-FOR-BOXY"));
        assertFalse(selector.selects(s));
    }

    @Test
    public void testPropertyNotPermittedSpecificSubject() {
        Resource resc = ResourceFactory.createResource();
        TombstonePropertySelector selector = new TombstonePropertySelector(resc);
        Statement s = ResourceFactory.createStatement(resc, Ebucore.privateTelephoneNumber,
                ResourceFactory.createPlainLiteral("800-FOR-BOXY"));
        assertFalse(selector.selects(s));
    }

}
