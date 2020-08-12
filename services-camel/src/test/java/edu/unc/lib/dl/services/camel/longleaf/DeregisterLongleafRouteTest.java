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
package edu.unc.lib.dl.services.camel.longleaf;

import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.CamelSpringRunner;
import org.apache.camel.test.spring.CamelTestContextBootstrapper;
import org.jgroups.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import edu.unc.lib.dl.services.MessageSender;

/**
 * @author bbpennel
 */
@RunWith(CamelSpringRunner.class)
@BootstrapWith(CamelTestContextBootstrapper.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/jms-context.xml"),
    @ContextConfiguration("/deregister-longleaf-router-context.xml")
})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class DeregisterLongleafRouteTest {
    private static final Logger log = getLogger(DeregisterLongleafRouteTest.class);

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private DeregisterLongleafProcessor deregisterLongleafProcessor;

    @Autowired
    private CamelContext cdrLongleaf;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private String longleafScript;
    private String outputPath;
    private List<String> output;

    @Before
    public void setup() throws Exception {
        tmpFolder.create();
        outputPath = tmpFolder.newFile().getPath();
        longleafScript = LongleafTestHelpers.getLongleafScript(outputPath);
        deregisterLongleafProcessor.setLongleafBaseCommand(longleafScript);
    }

    @Test
    public void deregisterSingleBinary() throws Exception {
        // Expecting 1 batch message and 1 individual file message, on different routes
        NotifyBuilder notify = new NotifyBuilder(cdrLongleaf)
                .whenCompleted(1 + 1)
                .create();

        String contentUri = generateContentUri();
        sendMessages(contentUri);

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Deregister route not satisfied", result1);

        assertDeregisterPaths(2000, contentUri);
    }

    @Test
    public void deregisterSingleBatch() throws Exception {
        // Expecting 1 batch message and 3 individual file messages, on different routes
        NotifyBuilder notify = new NotifyBuilder(cdrLongleaf)
                .whenCompleted(1 + 3)
                .create();

        String[] contentUris = generateContentUris(3);
        sendMessages(contentUris);

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Deregister route not satisfied", result1);

        assertDeregisterPaths(2000, contentUris);
    }

    @Test
    public void deregisterMultipleBatches() throws Exception {
        NotifyBuilder notify = new NotifyBuilder(cdrLongleaf)
                .whenCompleted(2 + 10)
                .create();

        String[] contentUris = generateContentUris(10);
        sendMessages(contentUris);

        boolean result1 = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Deregister route not satisfied", result1);

        assertDeregisterPaths(5000, contentUris);
    }

    private String generateContentUri() {
        return "file:///path/to/file/" + UUID.randomUUID().toString();
    }

    private String[] generateContentUris(int num) {
        String[] uris = new String[num];
        for (int i = 0; i < num; i++) {
            uris[i] = generateContentUri();
        }
        return uris;
    }

    private void sendMessages(String... contentUris) {
        for (String contentUri : contentUris) {
            messageSender.sendMessage(contentUri);
        }
    }

    /**
     * Assert that all of the provided content uris are present in the longleaf output
     * @param timeout time in milliseconds allowed for the condition to become true,
     *      to accommodate asynchronous unpredictable batch cutoffs
     * @param contentUris list of expected content uris
     * @throws Exception
     */
    private void assertDeregisterPaths(long timeout, String... contentUris) throws Exception {
        long start = System.currentTimeMillis();
        do {
            try {
                output = LongleafTestHelpers.readOutput(outputPath);
                assertDeregisterPaths(contentUris);
                return;
            } catch (AssertionError e) {
                if ((System.currentTimeMillis() - start) > timeout) {
                    throw e;
                }
                Thread.sleep(25);
                log.debug("DeregisterPaths not yet satisfied, retrying");
            }
        } while (true);
    }

    private void assertDeregisterPaths(String... contentUris) {
        for (String contentUri : contentUris) {
            URI uri = URI.create(contentUri);
            String contentPath = Paths.get(uri).toString();
            assertTrue("Expected content uri to be deregistered: " + contentPath, output.contains(contentPath));
        }
    }
}
