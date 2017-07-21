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

import java.net.URI;

import org.junit.Test;

public class URIUtilTest {

    @Test
    public void joinTest() {
        assertEquals("http://example.com",
                URIUtil.join("http://example.com/"));

        assertEquals("http://example.com/base",
                URIUtil.join("http://example.com/base/", new String[0]));

        assertEquals("http://example.com/base/path",
                URIUtil.join("http://example.com/base", "path"));

        assertEquals("http://example.com/base/path/to",
                URIUtil.join("http://example.com/", "base/", "/path", "to/"));

        assertEquals("path/to", URIUtil.join("", "path", "to"));
        
        assertEquals("path/to", URIUtil.join((String) null, "path", "to"));

        assertEquals("http://example.com/base/path",
                URIUtil.join(URI.create("http://example.com/base"), "path"));

        assertEquals("path",
                URIUtil.join((URI) null, "path"));
        
        assertEquals("http://example.com/base",
                URIUtil.join(URI.create("http://example.com/base"), (String) null));
        
        assertEquals("http://example.com/base/path/to/stuff",
                URIUtil.join(URI.create("http://example.com/base"), "path", (String) null, "to", "stuff"));
    }
    
    
}
