package edu.unc.lib.boxc.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.common.util.URIUtil;

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

        assertEquals("http://example.com/base/path/to/stuff?sort=dateAdded%2Freverse,",
                URIUtil.join(URI.create("http://example.com/base"), "path", "to",
                        "stuff?sort=dateAdded%2Freverse,", ""));
    }
    
    
}
