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
package edu.unc.lib.boxc.model.api.rdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;

/**
 * Utility containing common methods for manipulating and transforming RDF
 * models
 *
 * @author bbpennel
 *
 */
public class RDFModelUtil {

    public final static String TURTLE_MIMETYPE = "text/turtle";

    private RDFModelUtil() {
    }

    public static void serializeModel(Model model, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            RDFDataMgr.write(fos, model, RDFFormat.TURTLE_PRETTY);
        }
    }

    /**
     * Serializes and streams the provided model as serialized turtle
     *
     * @param model
     * @return
     * @throws IOException
     */
    public static InputStream streamModel(Model model) throws IOException {
        return streamModel(model, RDFFormat.TURTLE_PRETTY);
    }

    /**
     * Serializes and streams the provided model, using the specified format
     *
     * @param model
     * @param format
     * @return
     * @throws IOException
     */
    public static InputStream streamModel(Model model, RDFFormat format) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            RDFDataMgr.write(bos, model, format);
            return new ByteArrayInputStream(bos.toByteArray());
        }
    }

    /**
     * Serialize a model as a TTL string
     * @param model
     * @return string representation of the model
     * @throws IOException
     */
    public static String toString(Model model) throws IOException {
        InputStream stream = streamModel(model);
        return IOUtils.toString(stream, StandardCharsets.UTF_8);
    }

    /**
     * Returns a model built from the given turtle input stream
     *
     * @param inStream
     * @return
     */
    public static Model createModel(InputStream inStream) {
        return createModel(inStream, "TURTLE");
    }

    /**
     * Returns a model built from the given input stream using the specified language
     *
     * @param inStream
     * @param lang serialization language
     * @return
     */
    public static Model createModel(InputStream inStream, String lang) {
        try (InputStream stream = inStream) {
            Model model = ModelFactory.createDefaultModel();
            model.read(inStream, null, lang);
            return model;
        } catch (IOException e) {
            throw new RepositoryException("Failed to close model stream", e);
        }
    }

    /**
     * Returns a model built from the file at the given path
     *
     * @param filePath
     * @param lang serialization language
     * @return
     * @throws IOException
     */
    public static Model createModel(String filePath, String lang) throws IOException {
        return createModel(Files.newInputStream(Paths.get(filePath)), lang);
    }
}
