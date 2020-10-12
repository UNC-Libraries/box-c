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

import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders;

/**
 * Processor which validates and prepares image objects for producing derivatives
 *
 * @author bbpennel
 */
public class ImageDerivativeProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(ImageDerivativeProcessor.class);

    private static final Pattern MIMETYPE_PATTERN = Pattern.compile("^(image.*$|application.*?(photoshop|psd)$)");

    private static final Pattern DISALLOWED_PATTERN = Pattern.compile(".*(vnd\\.fpx).*");

    private static final Pattern RESTRICT_TO_FIRST_ENTRY_PATTERN =
            Pattern.compile("^(image|application).*?(photoshop|psd)$");

    /**
     * Returns true if the subject of the exchange is a binary which
     * is eligible for having image derivatives generated from it.
     *
     * @param exchange
     * @return
     */
    public static boolean allowedImageType(Exchange exchange) {
        Message in = exchange.getIn();
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);
        String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryPath);

        if (!MIMETYPE_PATTERN.matcher(mimetype).matches()) {
            log.debug("File type {} on object {} is not applicable for image derivatives", mimetype, binPath);
            return false;
        }

        if (DISALLOWED_PATTERN.matcher(mimetype).matches()) {
            log.debug("File type {} on object {} is disallowed for image derivatives", mimetype, binPath);
            return false;
        }

        log.debug("Object {} with type {} is permitted for image derivatives", binPath, mimetype);
        return true;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);

        String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryPath);
        if (RESTRICT_TO_FIRST_ENTRY_PATTERN.matcher(mimetype).matches()) {
            log.debug("Adjusting image path to {} for type {}", binPath + "[0]", mimetype);
            in.setHeader(CdrFcrepoHeaders.CdrImagePath, binPath + "[0]");
        } else {
            log.debug("Keeping existing image path as {} for type {}", binPath, mimetype);
            in.setHeader(CdrFcrepoHeaders.CdrImagePath, binPath);
        }
    }
}
