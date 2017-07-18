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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EntityTagTest {


    @Test
    public void strongEtagTest() {
        String value = "123456789";
        String header = '"' + value + '"';

        EntityTag etag = new EntityTag(header);
        assertEquals(value, etag.getValue());
        assertFalse(etag.isWeak());
    }

    @Test
    public void weakEtagTest() {
        String value = "123456789";
        String header = "W/\"" + value + '"';

        EntityTag etag = new EntityTag(header);
        assertEquals(value, etag.getValue());
        assertTrue(etag.isWeak());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidEtagTest() {
        String value = "123456789";

        new EntityTag(value);
    }
}
