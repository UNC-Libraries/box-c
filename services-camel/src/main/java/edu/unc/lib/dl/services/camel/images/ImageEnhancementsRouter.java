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
package edu.unc.lib.dl.services.camel.images;

import org.apache.camel.BeanInject;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;

/**
 * Router which triggers the creation of thumbnails when applicable binaries
 * are written.
 *
 * @author lfarrell
 */
public class ImageEnhancementsRouter extends RouteBuilder {

    private static final String MIMETYPE_PATTERN = "^(image.*$|application.*?(photoshop|psd)$)";

    @BeanInject(value = "addSmallThumbnailProcessor")
    private AddDerivativeProcessor addSmallThumbnailProcessor;

    @BeanInject(value = "addLargeThumbnailProcessor")
    private AddDerivativeProcessor addLargeThumbProcessor;

    @BeanInject(value = "addAccessCopyProcessor")
    private AddDerivativeProcessor addAccessCopyProcessor;

    /**
     * Configure the thumbnail route workflow.
     */
    @Override
    public void configure() throws Exception {
        onException(Exception.class)
            .redeliveryDelay("{{error.retryDelay}}")
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .backOffMultiplier("{{error.backOffMultiplier}}")
            .retryAttemptedLogLevel(LoggingLevel.WARN);

        from("direct-vm:process.enhancement.thumbnails")
            .routeId("ProcessThumbnails")
            .log(LoggingLevel.INFO, "Thumbs ${headers[CdrBinaryPath]} with ${headers[CdrMimeType]}")
            .filter(simple("${headers[CdrMimeType]} regex '" + MIMETYPE_PATTERN + "'"))
                .log(LoggingLevel.INFO, "Generating thumbnails for ${headers[org.fcrepo.jms.identifier]}"
                        + " of type ${headers[CdrMimeType]}")
                .multicast()
                .to("direct:small.thumbnail", "direct:large.thumbnail");

        from("direct:small.thumbnail")
            .routeId("SmallThumbnail")
            .log(LoggingLevel.INFO, "Creating/Updating Small Thumbnail for ${headers[CdrBinaryPath]}")
            .recipientList(simple("exec:/bin/sh?args=${properties:cdr.enhancement.bin}/convertScaleStage.sh "
                    + "${headers[CdrBinaryPath]} png 64 64 "
                    + "${properties:services.tempDirectory}/${headers[CdrCheckSum]}-small"))
            .bean(addSmallThumbnailProcessor);

        from("direct:large.thumbnail")
            .routeId("LargeThumbnail")
            .log(LoggingLevel.INFO, "Creating/Updating Large Thumbnail for ${headers[CdrBinaryPath]}")
            .recipientList(simple("exec:/bin/sh?args=${properties:cdr.enhancement.bin}/convertScaleStage.sh "
                    + "${headers[CdrBinaryPath]} png 128 128 "
                    + "${properties:services.tempDirectory}/${headers[CdrCheckSum]}-large"))
            .bean(addLargeThumbProcessor);

        from("direct-vm:process.enhancement.imageAccessCopy")
            .routeId("AccessCopy")
            .log(LoggingLevel.DEBUG, "Access copy triggered")
            .filter(simple("${headers[CdrMimeType]} regex '" + MIMETYPE_PATTERN + "'"))
                .log(LoggingLevel.INFO, "Creating/Updating JP2 access copy for ${headers[CdrBinaryPath]}")
                .recipientList(simple("exec:/bin/sh?args=${properties:cdr.enhancement.bin}/convertJp2.sh "
                        + "${headers[CdrBinaryPath]} jp2 "
                        + "${properties:services.tempDirectory}/${headers[CdrCheckSum]}-access"))
                .bean(addAccessCopyProcessor);
    }
}
