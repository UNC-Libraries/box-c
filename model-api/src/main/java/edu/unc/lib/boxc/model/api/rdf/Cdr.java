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

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definitions from /Users/bbpennel/Desktop/cdr-schemas/cdr.rdf
 * @author Auto-generated by schemagen on 05 May 2016 10:57
 */
public class Cdr {
    private Cdr() {
    }

    /** The namespace of the vocabulary as a string */
    public static final String NS = "http://cdr.unc.edu/definitions/model#";

    /** The namespace of the vocabulary as a string
     *  @see #NS */
    public static String getURI() {
        return NS; }

    /** The namespace of the vocabulary as a resource */
    public static final Resource NAMESPACE = createResource( NS );

    /** Name of the view to show by default on a collection record page. */
    public static final Property collectionDefaultView = createProperty(
            "http://cdr.unc.edu/definitions/model#collectionDefaultView" );

    /** Name of a view to show on this collections record page. Repeatable. */
    public static final Property collectionShowView = createProperty(
            "http://cdr.unc.edu/definitions/model#collectionShowView" );

    /** Method by which this deposit was submitted, such as "sword" or "CDR web form". */
    public static final Property depositMethod = createProperty(
            "http://cdr.unc.edu/definitions/model#depositMethod" );

    /** Subclassification of the packaging type for this deposit, such as a METS profile. */
    public static final Property depositPackageProfile = createProperty(
            "http://cdr.unc.edu/definitions/model#depositPackageProfile" );

    /** URI representing the type of packaging used for the original deposit represented
     *  by this record, such as CDR METS or BagIt.
     */
    public static final Property depositPackageType = createProperty(
            "http://cdr.unc.edu/definitions/model#depositPackageType" );

    /** User who this deposit was submitted on behalf of. */
    public static final Property depositedOnBehalfOf = createProperty(
            "http://cdr.unc.edu/definitions/model#depositedOnBehalfOf" );

    /** Relationship indicating ownership of a manifest by this deposit record */
    public static final Property hasManifest = createProperty(
            "http://cdr.unc.edu/definitions/model#hasManifest" );

    /** Relationship indicating a checksum on this binary object */
    public static final Property hasMessageDigest = createProperty(
            "http://cdr.unc.edu/definitions/model#hasMessageDigest" );

    /** Relationship indicating the resource containing the MODS description for this object */
   public static final Property hasMods = createProperty("http://cdr.unc.edu/definitions/model#hasMods");

   /** Relationship indicating the resource containing the event log for this object */
   public static final Property hasEvents = createProperty("http://cdr.unc.edu/definitions/model#hasEvents");

   /** The size (e.g., in bytes) of this binary object */
   public static final Property hasSize = createProperty(
           "http://cdr.unc.edu/definitions/model#hasSize" );

   /** Identifier indicating the profile of the metadata provided with this object
     *  at deposit time. Used to identify what transformation was used to generate
     *  descriptive metadata from the original metadata.
     */
    public static final Property hasSourceMetadataProfile = createProperty(
            "http://cdr.unc.edu/definitions/model#hasSourceMetadataProfile" );

    /**
     * Refers to the list of PIDs of parent objects that contained an object slated
     * for removal from the repository. Includes PIDs of objects beginning from
     * the immediate parent all the way to the root of the repository.
     */
    public static final Property historicalIdPath = createProperty(
            "http://cdr.unc.edu/definitions/model#historicalIdPath" );

    /**
     * Refers to the list of names of parent objects that contained an object slated
     * for removal from the repository. Includes names of objects beginning from
     * the immediate parent all the way to the root of the repository.
     */
    public static final Property historicalPath = createProperty(
            "http://cdr.unc.edu/definitions/model#historicalPath" );

    /**
     * Property containing the identifier of the storage location in which content
     * for this resource is located.
     */
    public static final Property storageLocation = createProperty(
            "http://cdr.unc.edu/definitions/model#storageLocation" );

    /** Reference to a vocabulary object. For objects in this collection, if the given
     *  vocabulary applies to a descriptive field it will only index its value if
     *  it is found within the vocabulary.
     */
    public static final Property indexValidTerms = createProperty(
            "http://cdr.unc.edu/definitions/model#indexValidTerms" );

    /** An invalid vocabulary term in this descriptive record. */
    public static final Property invalidTerm = createProperty(
            "http://cdr.unc.edu/definitions/model#invalidTerm" );

    /** Relation which denotes the deposit record for the subject object. */
    public static final Property originalDeposit = createProperty(
            "http://cdr.unc.edu/definitions/model#originalDeposit" );

    /** Property which records custom ordering of members within a container
     */
    public static final Property memberOrder = createProperty(
            "http://cdr.unc.edu/definitions/model#memberOrder" );

    /** Relation from a work to a child which will be treated as the primary
     *  object for this work.
     */
    public static final Property primaryObject = createProperty(
            "http://cdr.unc.edu/definitions/model#primaryObject" );

    /** Reference to a vocabulary object. For objects being deposited into this collection,
     *  attempt to remap invalid terms to valid terms when possible.
     */
    public static final Property replaceInvalidTerms = createProperty(
            "http://cdr.unc.edu/definitions/model#replaceInvalidTerms" );

    /**
     *
     */
    public static final Property unpublished = createProperty(
            "http://cdr.unc.edu/definitions/model#unpublished" );

    /** Selector for identifying fields within descriptive metadata records which
     *  this vocabulary applies to.
     */
    public static final Property vocabularySelector = createProperty(
            "http://cdr.unc.edu/definitions/model#vocabularySelector" );

    /** Indicates the encoding of terms within this vocabulary. */
    public static final Property vocabularyType = createProperty(
            "http://cdr.unc.edu/definitions/model#vocabularyType" );

    /** URI identifying this vocabulary. */
    public static final Property vocabularyUri = createProperty(
            "http://cdr.unc.edu/definitions/model#vocabularyUri" );

    /** Reference to a vocabulary object. For objects in this collection, display
     *  warnings in the admin interface if there are invalid vocabulary terms.
     */
    public static final Property warnInvalidTerms = createProperty(
            "http://cdr.unc.edu/definitions/model#warnInvalidTerms" );

    public static final Resource FileObject = createResource(
            "http://cdr.unc.edu/definitions/model#FileObject" );
    public static final Resource Work = createResource(
            "http://cdr.unc.edu/definitions/model#Work" );
    public static final Resource Folder = createResource(
            "http://cdr.unc.edu/definitions/model#Folder" );
    public static final Resource Collection = createResource(
            "http://cdr.unc.edu/definitions/model#Collection" );
    public static final Resource AdminUnit = createResource(
            "http://cdr.unc.edu/definitions/model#AdminUnit" );
    public static final Resource ContentRoot = createResource(
            "http://cdr.unc.edu/definitions/model#ContentRoot" );
    public static final Resource DepositRecord = createResource(
            "http://cdr.unc.edu/definitions/model#DepositRecord" );
    public static final Resource SourceMetadata = createResource(
            "http://cdr.unc.edu/definitions/model#SourceMetadata" );
    public static final Resource DescriptiveMetadata = createResource(
            "http://cdr.unc.edu/definitions/model#DescriptiveMetadata" );
    public static final Resource Tombstone = createResource(
            "http://cdr.unc.edu/definitions/model#Tombstone" );
}
