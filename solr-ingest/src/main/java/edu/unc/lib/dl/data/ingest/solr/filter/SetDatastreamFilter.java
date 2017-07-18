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
package edu.unc.lib.dl.data.ingest.solr.filter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.rdf.Ebucore;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.search.solr.model.Datastream;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 * Extracts datastreams from an object and sets related properties concerning the default datastream for the object.
 *
 * Sets datastream, filesizeTotal, filesizeSort
 *
 * @author bbpennel
 *
 */
public class SetDatastreamFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetDatastreamFilter.class);

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing Datastream filter for object", dip.getPid());

        ContentObject contentObj = dip.getContentObject();

        FileObject fileObj = getFileObject(contentObj);
        if (fileObj == null) {
            return;
        }

        boolean ownedByOtherObject = contentObj instanceof WorkObject;
        List<Datastream> datastreams = getDatastreams(fileObj, ownedByOtherObject);

        IndexDocumentBean doc = dip.getDocument();

        doc.setDatastream(getDatastreamStrings(datastreams));
        doc.setFilesizeTotal(getFilesizeTotal(datastreams));
        doc.setFilesizeSort(getFilesize(datastreams));
    }

    private FileObject getFileObject(ContentObject contentObj) {
        if (contentObj instanceof FileObject) {
            return (FileObject) contentObj;
        } else if (contentObj instanceof WorkObject) {
            WorkObject workObj = (WorkObject) contentObj;
            return workObj.getPrimaryObject();
        } else {
            // object being indexed must be a work or a file object
            return null;
        }
    }

    /**
     * Generates a list of Datastream objects from each binary belonging to the
     * provided FileObject. If the datastreams are being recorded on an object
     * other than their owning file object, the pid of the owning file object is
     * recorded
     * 
     * @param fileObj
     * @param ownedByOtherObject
     * @return
     */
    private List<Datastream> getDatastreams(FileObject fileObj, boolean ownedByOtherObject) {
        return fileObj.getBinaryObjects().stream()
            .map(binary -> {
                Resource binaryResc = binary.getResource();

                String name = binaryResc.getURI();
                name = name.substring(name.lastIndexOf('/') + 1);

                String mimetype = binaryResc.getProperty(Ebucore.hasMimeType).getString();
                long filesize = binaryResc.getProperty(Premis.hasSize).getLong();
                // Making assumption that there is only one checksum
                String checksum = binaryResc.getProperty(Premis.hasMessageDigest).getResource().getURI();

                String filename = binaryResc.getProperty(Ebucore.filename).getString();
                int extensionIndex = filename.lastIndexOf('.');
                String extension = extensionIndex == -1 ? "" : filename.substring(extensionIndex + 1);

                String owner = ownedByOtherObject ? fileObj.getPid().getId() : null;

                return new Datastream(owner, name, filesize, mimetype, filename, extension, checksum);
            }).collect(Collectors.toList());
    }

    private List<String> getDatastreamStrings(List<Datastream> datastreams) {
        return datastreams.stream()
                .map(Datastream::toString)
                .collect(Collectors.toList());
    }

    /**
     * Returns the sum of filesizes for all datastreams which do no belong to
     * other objects
     * 
     * @param datastreams
     * @return
     */
    private long getFilesizeTotal(List<Datastream> datastreams) {
        return datastreams.stream()
            .filter(ds -> ds.getFilesize() != null && ds.getOwner() == null)
            .mapToLong(ds -> ds.getFilesize())
            .sum();
    }

    private long getFilesize(List<Datastream> datastreams) throws IndexingException {
        Optional<Datastream> original = datastreams.stream()
                .filter(ds -> RepositoryPathConstants.ORIGINAL_FILE.equals(ds.getName()))
                .findFirst();

        if (!original.isPresent()) {
            throw new IndexingException("File object in invalid state, cannot find original file binary");
        }

        return original.get().getFilesize();
    }
}
