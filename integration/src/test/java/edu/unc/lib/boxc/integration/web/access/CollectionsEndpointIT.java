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
package edu.unc.lib.boxc.integration.web.access;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.integration.factories.AdminUnitFactory;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
@ContextHierarchy({
        @ContextConfiguration("/spring-test/test-fedora-container.xml"),
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/spring-test/acl-service-context.xml"),
        @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
        @ContextConfiguration("/spring-test/object-factory-context.xml")
})
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class CollectionsEndpointIT {
    @Autowired
    private EmbeddedSolrServer solrServer;
    private Server webServer;

    @Autowired
    private AdminUnitFactory adminUnitFactory;

    protected final static String USERNAME = "test_user";
    protected final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    @Autowired(required = false)
    protected String baseAddress;

    @Before
    public void setup() throws Exception {
        webServer = new Server(48080);
        setUpSystemProperties(webServer);
        webServer.setStopAtShutdown(true);
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/");
        webAppContext.setResourceBase("../web-access-app/src/main/webapp");
        webAppContext.setClassLoader(getClass().getClassLoader());
        webServer.setHandler(webAppContext);
        webServer.start();

        TestHelper.setContentBase("http://localhost:48085/rest");

        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
    }

    @After
    public void shutdownServer() throws Exception {
        webServer.stop();
    }

    private void setUpSystemProperties(Server jettyServer) {
        final Properties systemProperties = new Properties();
        // set your system properties...
        String classpath = new File(".").getAbsolutePath();
        systemProperties.setProperty("server.properties.uri", "file:" + classpath + "/src/test/resources/access-app.properties");
        systemProperties.setProperty("acl.properties.uri", "file:" + classpath + "/src/test/resources/empty.properties");
        systemProperties.setProperty("acl.patronPrincipalConfig.path", classpath + "/src/test/resources/patronPrincipalConfig.json");
        jettyServer.addLifeCycleListener(new SystemPropertiesLifeCycleListener(systemProperties));
    }

    private class SystemPropertiesLifeCycleListener extends AbstractLifeCycle.AbstractLifeCycleListener {
        private Properties toSet;

        public SystemPropertiesLifeCycleListener(Properties toSet) {
            this.toSet = toSet;
        }

        @Override
        public void lifeCycleStarting(LifeCycle anyLifeCycle) {
            // add to (don't replace) System.getProperties()
            System.getProperties().putAll(toSet);
        }
    }

    @Test
    public void testCollections() throws Exception {
        var adminUnit = adminUnitFactory.createAdminUnit(Map.of("title", "title1"));
        var httpClient = HttpClients.createDefault();

        HttpGet method = new HttpGet("http://localhost:48080/collectionsJson");
        try (CloseableHttpResponse httpResp = httpClient.execute(method)) {
            var statusCode = httpResp.getStatusLine().getStatusCode();
            assertEquals(200, statusCode);
        }
    }
}
