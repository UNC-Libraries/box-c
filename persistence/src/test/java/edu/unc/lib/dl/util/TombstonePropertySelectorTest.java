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
package edu.unc.lib.dl.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.junit.Test;

import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.Ebucore;

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
