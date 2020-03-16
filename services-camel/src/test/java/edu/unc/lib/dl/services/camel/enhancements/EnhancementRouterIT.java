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
package edu.unc.lib.dl.services.camel.enhancements;

import static edu.unc.lib.dl.model.DatastreamPids.getTechnicalMetadataPid;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Container;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.EVENT_TYPE;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.IDENTIFIER;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.RESOURCE_TYPE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import edu.unc.lib.dl.services.camel.BinaryEnhancementProcessor;
import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.services.camel.BinaryMetadataProcessor;
import edu.unc.lib.dl.services.camel.fulltext.FulltextProcessor;
import edu.unc.lib.dl.services.camel.images.AddDerivativeProcessor;
import edu.unc.lib.dl.services.camel.solr.SolrIngestProcessor;
import edu.unc.lib.dl.test.TestHelper;

/**
 *
 * @author bbpennel
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/jms-context.xml"),
    @ContextConfiguration("/enhancement-router-it-context.xml")
})
public class EnhancementRouterIT {

    private final static String FILE_CONTENT = "content";

    @Autowired
    private String baseAddress;

    @Autowired
    private RepositoryObjectFactory repoObjectFactory;

    @Autowired
    private CamelContext cdrEnhancements;

    @Produce(uri = "direct-vm:enhancements.fedora")
    private ProducerTemplate template;

    @BeanInject(value = "addSmallThumbnailProcessor")
    private AddDerivativeProcessor addSmallThumbnailProcessor;

    @BeanInject(value = "addLargeThumbnailProcessor")
    private AddDerivativeProcessor addLargeThumbnailProcessor;

    @BeanInject(value = "addAccessCopyProcessor")
    private AddDerivativeProcessor addAccessCopyProcessor;

    @BeanInject(value = "solrIngestProcessor")
    private SolrIngestProcessor solrIngestProcessor;

    @BeanInject(value = "fulltextProcessor")
    private FulltextProcessor fulltextProcessor;

    @BeanInject(value = "binaryMetadataProcessor")
    private BinaryMetadataProcessor binaryMetadataProcessor;

    @Before
    public void init() throws Exception {
        initMocks(this);

        reset(solrIngestProcessor);

        TestHelper.setContentBase(baseAddress);

        File thumbScriptFile = new File("target/convertScaleStage.sh");
        FileUtils.writeStringToFile(thumbScriptFile, "exit 0", "utf-8");
        thumbScriptFile.deleteOnExit();

        File jp2ScriptFile = new File("target/convertJp2.sh");
        FileUtils.writeStringToFile(jp2ScriptFile, "exit 0", "utf-8");
        jp2ScriptFile.deleteOnExit();
    }

    @Test
    public void testFolderEnhancements() throws Exception {
        FolderObject folderObject = repoObjectFactory.createFolderObject(null);

        final Map<String, Object> headers = createEvent(folderObject.getPid(),
                Cdr.Folder.getURI(), Container.getURI());
        template.sendBodyAndHeaders("", headers);

        NotifyBuilder notify = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(3)
                .create();

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        verify(solrIngestProcessor).process(any(Exchange.class));
    }

    @Test
    public void testImageFile() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        Path originalPath = Files.createTempFile("file", ".png");
        FileUtils.writeStringToFile(originalPath.toFile(), FILE_CONTENT, "UTF-8");
        BinaryObject binObj = fileObj.addOriginalFile(originalPath.toUri(),
                null, "image/png", null, null);

        final Map<String, Object> headers = createEvent(binObj.getPid(), Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        // Separate exchanges when multicasting
        NotifyBuilder notify = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(2)
                .create();

        boolean result = notify.matches(5l, TimeUnit.SECONDS);
        assertTrue("Processing message did not match expectations", result);

        verify(addSmallThumbnailProcessor).process(any(Exchange.class));
        verify(addLargeThumbnailProcessor).process(any(Exchange.class));
        verify(addAccessCopyProcessor).process(any(Exchange.class));
        // Indexing not triggered on binary object
        verify(solrIngestProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void testBinaryMetadataFile() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        Path originalPath = Files.createTempFile("file", ".png");
        FileUtils.writeStringToFile(originalPath.toFile(), FILE_CONTENT, "UTF-8");
        BinaryObject binObj = fileObj.addOriginalFile(originalPath.toUri(),
                null, "image/png", null, null);

        String mdId = binObj.getPid().getRepositoryPath() + "/fcr:metadata";
        PID mdPid = PIDs.get(mdId);

        final Map<String, Object> headers = createEvent(mdPid, Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        // Separate exchanges when multicasting
        NotifyBuilder notify = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(2)
                .create();

        boolean result = notify.matches(5l, TimeUnit.SECONDS);

        assertTrue("Processing message did not match expectations", result);

        verify(addSmallThumbnailProcessor, never()).process(any(Exchange.class));
        verify(addLargeThumbnailProcessor, never()).process(any(Exchange.class));
        verify(addAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(solrIngestProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void testInvalidFile() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        PID fitsPid = getTechnicalMetadataPid(fileObj.getPid());
        Path techmdPath = Files.createTempFile("fits", ".xml");
        FileUtils.writeStringToFile(techmdPath.toFile(), FILE_CONTENT, "UTF-8");
        BinaryObject binObj = fileObj.addBinary(fitsPid, techmdPath.toUri(),
                "fits.xml", "text/xml", null, null, null);

        final Map<String, Object> headers = createEvent(binObj.getPid(), Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        NotifyBuilder notify = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(1)
                .create();

        boolean result = notify.matches(5l, TimeUnit.SECONDS);

        assertTrue("Processing message did not match expectations", result);

        verify(addSmallThumbnailProcessor, never()).process(any(Exchange.class));
        verify(fulltextProcessor,  never()).process(any(Exchange.class));
        verify(solrIngestProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void testProcessFilterOutDescriptiveMDSolr() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        Path modsPath = Files.createTempFile(null, null);
        writeStringToFile(modsPath.toFile(), FILE_CONTENT, UTF_8);
        BinaryObject descObj = fileObj.setDescription(modsPath.toUri());

        Map<String, Object> headers = createEvent(descObj.getPid(),
                Cdr.FileObject.getURI(), Cdr.DescriptiveMetadata.getURI());
        template.sendBodyAndHeaders("", headers);

        NotifyBuilder notify = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(1)
                .create();

        boolean result = notify.matches(5l, TimeUnit.SECONDS);

        assertTrue("Processing message did not match expectations", result);

        verify(solrIngestProcessor, never()).process(any(Exchange.class));
    }

    private static Map<String, Object> createEvent(PID pid, String... type) {

        final Map<String, Object> headers = new HashMap<>();
        headers.put(IDENTIFIER, pid.getRepositoryPath());
        headers.put(EVENT_TYPE, "ResourceCreation");
        headers.put("CamelFcrepoUri", pid.getRepositoryPath());
        headers.put(RESOURCE_TYPE, String.join(",", type));

        return headers;
    }
}
