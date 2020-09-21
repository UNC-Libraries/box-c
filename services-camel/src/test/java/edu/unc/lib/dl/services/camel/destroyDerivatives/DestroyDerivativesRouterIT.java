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
package edu.unc.lib.dl.services.camel.destroyDerivatives;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.services.DestroyObjectsMessageHelpers;
import edu.unc.lib.dl.test.TestHelper;

/**
 *
 * @author lfarrell
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("/spring-test/test-fedora-container.xml"),
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/spring-test/jms-context.xml"),
        @ContextConfiguration("/destroy-derivatives-router-it-context.xml")
})
public class DestroyDerivativesRouterIT {
    private final static String FILE_CONTENT = "content";

    @Autowired
    private String baseAddress;

    @Autowired
    private RepositoryObjectFactory repoObjectFactory;

    @Autowired
    private CamelContext cdrDestroyDerivatives;

    @Produce(uri = "{{cdr.destroy.derivatives.stream.camel}}")
    private ProducerTemplate template;

    @BeanInject(value = "binaryInfoProcessor")
    private BinaryInfoProcessor binaryInfoProcessor;

    @BeanInject(value = "destroySmallThumbnailProcessor")
    private DestroyDerivativesProcessor destroySmallThumbnailProcessor;

    @BeanInject(value = "destroyLargeThumbnailProcessor")
    private DestroyDerivativesProcessor destroyLargeThumbnailProcessor;

    @BeanInject(value = "destroyAccessCopyProcessor")
    private DestroyDerivativesProcessor destroyAccessCopyProcessor;

    @BeanInject(value = "destroyFulltextProcessor")
    private DestroyDerivativesProcessor destroyFulltextProcessor;

    @Before
    public void init() {
        initMocks(this);

        reset(binaryInfoProcessor);
        reset(destroySmallThumbnailProcessor);
        reset(destroyLargeThumbnailProcessor);
        reset(destroyAccessCopyProcessor);
        reset(destroyFulltextProcessor);

        TestHelper.setContentBase(baseAddress);
    }

    @Test
    public void destroyImageTest() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        Path originalPath = Files.createTempFile("file", ".png");
        FileUtils.writeStringToFile(originalPath.toFile(), FILE_CONTENT, "UTF-8");
        fileObj.addOriginalFile(originalPath.toUri(),
                null, "image/png", null, null);

        createAndSendMessages(fileObj);

        verify(binaryInfoProcessor).process(any(Exchange.class));
        verify(destroySmallThumbnailProcessor).process(any(Exchange.class));
        verify(destroyLargeThumbnailProcessor).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor).process(any(Exchange.class));
        verify(destroyFulltextProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void destroyTextTest() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        Path originalPath = Files.createTempFile("file", ".png");
        FileUtils.writeStringToFile(originalPath.toFile(), FILE_CONTENT, "UTF-8");
        fileObj.addOriginalFile(originalPath.toUri(),
                null, "text/plain", null, null);

        createAndSendMessages(fileObj);

        verify(binaryInfoProcessor).process(any(Exchange.class));
        verify(destroySmallThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyLargeThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(destroyFulltextProcessor).process(any(Exchange.class));
    }

    @Test
    public void invalidTypeTest() throws Exception {
        FileObject fileObj = repoObjectFactory.createFileObject(null);
        Path originalPath = Files.createTempFile("file", ".png");
        FileUtils.writeStringToFile(originalPath.toFile(), FILE_CONTENT, "UTF-8");
        fileObj.addOriginalFile(originalPath.toUri(),
                null, "application/octet-stream", null, null);

        createAndSendMessages(fileObj);

        verify(binaryInfoProcessor).process(any(Exchange.class));
        verify(destroySmallThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyLargeThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(destroyFulltextProcessor, never()).process(any(Exchange.class));
    }

    private void createAndSendMessages(FileObject fileObj) {
        Map<URI, Map<String, String>> objsToDestroy = derivativesToCleanup(fileObj.getBinaryObjects());
        objsToDestroy.forEach((contentUri, metadata) -> {
            Document msg = DestroyObjectsMessageHelpers
                    .makeDestroyOperationBody("test_user", contentUri, metadata);
            template.sendBody(msg);
        });
    }

    private Map<URI, Map<String, String>> derivativesToCleanup(List<BinaryObject> binaries) {
        HashMap<URI, Map<String, String>> cleanupBinaryUris = new HashMap<>();

        for (BinaryObject binary : binaries) {
            Map<String, String> contentMetadata = new HashMap<>();
            contentMetadata.put("pid", binary.getPid().getQualifiedId());
            contentMetadata.put("mimeType", binary.getMimetype());

            cleanupBinaryUris.put(binary.getContentUri(), contentMetadata);
        }

        return cleanupBinaryUris;
    }
}
