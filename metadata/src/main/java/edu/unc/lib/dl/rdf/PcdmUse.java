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
package edu.unc.lib.dl.rdf;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import org.apache.jena.rdf.model.Resource;

/**
 * Portland Common Data Model: Use Extension
 *
 * @author Generated from
 * https://github.com/duraspace/pcdm/blob/master/pcdm-ext/use.rdf
 */
public class PcdmUse {
    private PcdmUse() {
    }

    /** The namespace of the vocabulary as a string */
    public static final String NS = "http://pcdm.org/use#";

    /**
     * The namespace of the vocabulary as a string
     *
     * @see #NS
     */
    public static String getURI() {
        return NS;
    }

    /** The namespace of the vocabulary as a resource */
    public static final Resource NAMESPACE = createResource(NS);

    /**
     * A textual representation of the Object appropriate for fulltext indexing,
     * such as a plaintext version of a document, or OCR text.
     */
    public static final Resource ExtractedText = createResource("http://pcdm.org/use#ExtractedText");

    /**
     * High quality representation of the Object, appropriate for generating
     * derivatives or other additional processing.
     */
    public static final Resource IntermediateFile = createResource("http://pcdm.org/use#IntermediateFile");

    /** The original creation format of a file. */
    public static final Resource OriginalFile = createResource("http://pcdm.org/use#OriginalFile");

    /**
     * Best quality representation of the Object appropriate for long-term
     * preservation.
     */
    public static final Resource PreservationMasterFile = createResource("http://pcdm.org/use#PreservationMasterFile");

    /**
     * A medium quality representation of the Object appropriate for serving to
     * users. Similar to a FADGI "derivative file" but can also be used for
     * born-digital content, and is not necessarily derived from another file.
     */
    public static final Resource ServiceFile = createResource("http://pcdm.org/use#ServiceFile");

    /**
     * A low resolution image representation of the Object appropriate for using
     * as an icon.
     */
    public static final Resource ThumbnailImage = createResource("http://pcdm.org/use#ThumbnailImage");

    /**
     * A textual representation of the Object appropriate for presenting to
     * users, such as subtitles or transcript of a video. Can be used as a
     * substitute or complement to other files for accessibility purposes.
     */
    public static final Resource Transcript = createResource("http://pcdm.org/use#Transcript");
}
