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
package edu.unc.lib.dl.services.camel;

import static edu.unc.lib.dl.rdf.Ebucore.hasMimeType;
import static edu.unc.lib.dl.rdf.Premis.hasMessageDigest;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryChecksum;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryUri;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.StringJoiner;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.rdf.Fcrepo4Repository;

/**
 * Stores information related to identifying binary objects from the repository
 *
 * @author lfarrell
 *
 */
public class BinaryMetadataProcessor implements Processor {

    private final int BINARY_PATH_DEPTH = 3;
    private final int BINARY_PATH_LENGTH = 2;

    private String baseBinaryPath;

    protected BinaryMetadataProcessor(String baseBinaryPath) {
        this.baseBinaryPath = baseBinaryPath;
        if (!baseBinaryPath.endsWith("/")) {
            this.baseBinaryPath += "/";
        }
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        final Model model = createDefaultModel();

        String fcrepoBinaryUri = (String) in.getHeader("CamelFcrepoUri");

        Model values = model.read(in.getBody(InputStream.class), null, "Turtle");
        ResIterator resources = values.listResourcesWithProperty(RDF.type, Fcrepo4Repository.Binary);

        try {
            if (resources.hasNext()) {
                Resource resource = resources.next();
                String binaryMimeType = resource.getProperty(hasMimeType).getObject().toString();
                String binaryFcrepoChecksum = resource.getProperty(hasMessageDigest).getObject().toString();

                String[] binaryFcrepoChecksumSplit = binaryFcrepoChecksum.split(":");

                String binaryPath = idToPath(binaryFcrepoChecksumSplit[2], BINARY_PATH_DEPTH, BINARY_PATH_LENGTH);

                String binaryFullPath = new StringJoiner("")
                    .add(baseBinaryPath)
                    .add(binaryPath)
                    .add(binaryFcrepoChecksumSplit[2])
                    .toString();
                // Only set the binary path if the computed path exists
                if (Files.exists(Paths.get(binaryFullPath))) {
                    in.setHeader(CdrBinaryPath, binaryFullPath);
                }

                in.setHeader(CdrBinaryChecksum, binaryFcrepoChecksumSplit[2]);
                in.setHeader(CdrBinaryMimeType, binaryMimeType);
                in.setHeader(CdrBinaryUri, fcrepoBinaryUri);
            }
        } finally {
            resources.close();
        }
    }

    /**
     * Prepend id with defined levels of hashed containers based on the values.
     * For example, 9bd8b60e-93a2-4b66-8f0a-b62338483b39 would become
     *    9b/d8/b6/9bd8b60e-93a2-4b66-8f0a-b62338483b39
     *
     * @param id
     * @return
     */
    private String idToPath(String id, int pathDepth, int length) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < pathDepth; i++) {
            sb.append(id.substring(i * length, i * length + length))
                    .append('/');
        }

        return sb.toString();
    }

}
