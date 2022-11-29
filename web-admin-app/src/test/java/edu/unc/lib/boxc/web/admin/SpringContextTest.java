package edu.unc.lib.boxc.web.admin;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/solr-search-context.xml",
        "file:src/main/webapp/WEB-INF/uiapp-servlet.xml",
        "file:src/main/webapp/WEB-INF/service-context.xml",
        "file:src/main/webapp/WEB-INF/access-fedora-context.xml" })
public class SpringContextTest {
    @BeforeClass
    public static void setup() {
        System.setProperty("acl.properties.uri", "acl-config.properties");
    }

    @Test
    public void testLoadSpringContext() {
        // nothing to do here
    }
}
