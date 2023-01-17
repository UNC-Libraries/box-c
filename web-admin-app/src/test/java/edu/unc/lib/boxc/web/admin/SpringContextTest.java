package edu.unc.lib.boxc.web.admin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/solr-search-context.xml",
        "file:src/main/webapp/WEB-INF/uiapp-servlet.xml",
        "file:src/main/webapp/WEB-INF/service-context.xml",
        "file:src/main/webapp/WEB-INF/access-fedora-context.xml" })
public class SpringContextTest {
    @BeforeAll
    public static void setup() {
        System.setProperty("acl.properties.uri", "acl-config.properties");
    }

    @Test
    public void testLoadSpringContext() {
        // nothing to do here
    }
}
