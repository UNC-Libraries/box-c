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
package edu.unc.lib.dl.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

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
     * Returns a model built from the given turtle input stream
     *
     * @param inStream
     * @return
     */
    public static Model createModel(InputStream inStream) {
        Model model = ModelFactory.createDefaultModel();
        model.read(inStream, null, "TURTLE");
        return model;
    }


}
