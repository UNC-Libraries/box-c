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
package edu.unc.lib.dl.xml;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.TestCase;

/**
 * Unit tests to ensure that namespace uris have valid syntax.
 */
public class NamespaceConstantsTest extends TestCase {

    /**
     * Tests that fields ending in "_URI" can all be parsed by java.net.URI
     * constructor.
     */
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
