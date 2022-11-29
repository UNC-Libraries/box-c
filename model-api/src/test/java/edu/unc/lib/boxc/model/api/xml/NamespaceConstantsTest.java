package edu.unc.lib.boxc.model.api.xml;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

/**
 * Unit tests to ensure that namespace uris have valid syntax.
 */
public class NamespaceConstantsTest {

    /**
     * Tests that fields ending in "_URI" can all be parsed by java.net.URI
     * constructor.
     */
    @Test
    public void testConstantURIs() {
        Class<NamespaceConstants> nsClass = NamespaceConstants.class;
        for (Field field : nsClass.getDeclaredFields()) {
            if (field.getName().endsWith("URI")) {
            try {
                new URI((String) field.get(null));
                assertTrue(true);
            } catch (URISyntaxException e) {
                fail("URI constant '" + field.getName() + "' is not a valid URI");
            } catch (IllegalAccessException e) {
                fail("URI constant " + field.getName() + " is not accessible!");
            }
            }
        }
    }
}
