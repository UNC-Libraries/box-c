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

import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Container;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.EVENT_TYPE;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.IDENTIFIER;
import static edu.unc.lib.dl.services.camel.JmsHeaderConstants.RESOURCE_TYPE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.services.camel.BinaryMetadataProcessor;
import edu.unc.lib.dl.services.camel.GetBinaryProcessor;
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
    @ContextConfiguration("/enhancement-router-it-context.xml")
})
public class EnhancementRouterIT {

    private final static String FILE_CONTENT = "content";

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    private String baseBinaryPath;

    @Autowired
    private String baseAddress;

    @Autowired
    private RepositoryObjectFactory repoObjectFactory;

    @Autowired
    private CamelContext cdrEnhancements;

    @Autowired
    private CamelContext cdrServiceImageEnhancements;

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

    @BeanInject(value = "getBinaryProcessor")
    private GetBinaryProcessor getBinaryProcessor;

    @Before
    public void init() throws Exception {
        initMocks(this);

        reset(solrIngestProcessor);

        TestHelper.setContentBase(baseAddress);

        baseBinaryPath = tmpFolder.getRoot().getAbsolutePath();
        getBinaryProcessor.setTempDirectory(baseBinaryPath);

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

        notify.matches(5l, TimeUnit.SECONDS);

        verify(solrIngestProcessor).process(any(Exchange.class));
    }

    @Test
    public void testImageFile() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        BinaryObject binObj = fileObj.addOriginalFile(new ByteArrayInputStream(FILE_CONTENT.getBytes()),
                null, "image/png", null, null);

        final Map<String, Object> headers = createEvent(binObj.getPid(), Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        // Separate exchanges when multicasting
        NotifyBuilder notify = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(2)
                .create();

        notify.matches(5l, TimeUnit.SECONDS);

        verify(addSmallThumbnailProcessor).process(any(Exchange.class));
        verify(addLargeThumbnailProcessor).process(any(Exchange.class));
        verify(addAccessCopyProcessor).process(any(Exchange.class));
        // Indexing not triggered on binary object
        verify(solrIngestProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void testInvalidFile() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        BinaryObject binObj = fileObj.addBinary("techmd_fits", new ByteArrayInputStream(FILE_CONTENT.getBytes()),
                "fits.xml", "text/xml", null, null, null);

        final Map<String, Object> headers = createEvent(binObj.getPid(), Binary.getURI());
        template.sendBodyAndHeaders("", headers);

        NotifyBuilder notify = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(1)
                .create();

        notify.matches(5l, TimeUnit.SECONDS);

        verify(addSmallThumbnailProcessor, never()).process(any(Exchange.class));
        verify(fulltextProcessor,  never()).process(any(Exchange.class));
        verify(solrIngestProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void testProcessFilterOutDescriptiveMDSolr() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        FileObject descObj = fileObj.setDescription(new ByteArrayInputStream(FILE_CONTENT.getBytes()));

        Map<String, Object> headers = createEvent(descObj.getPid(),
                Cdr.FileObject.getURI(), Cdr.DescriptiveMetadata.getURI());
        template.sendBodyAndHeaders("", headers);

        NotifyBuilder notify = new NotifyBuilder(cdrEnhancements)
                .whenCompleted(1)
                .create();

        notify.matches(5l, TimeUnit.SECONDS);

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
