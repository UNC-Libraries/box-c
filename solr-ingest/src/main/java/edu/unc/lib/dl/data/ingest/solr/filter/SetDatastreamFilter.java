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

import static edu.unc.lib.dl.model.DatastreamType.ORIGINAL_FILE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamType;
import edu.unc.lib.dl.rdf.Ebucore;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.search.solr.model.Datastream;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.DerivativeService;

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

    private DerivativeService derivativeService;

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing Datastream filter for object {}", dip.getPid());

        ContentObject contentObj = dip.getContentObject();
        IndexDocumentBean doc = dip.getDocument();

        List<Datastream> datastreams = new ArrayList<>();

        FileObject fileObj = getFileObject(contentObj);
        if (fileObj != null) {
            boolean ownedByOtherObject = contentObj instanceof WorkObject;

            // Add list of file datastreams associated with this object
            addDatastreams(datastreams, fileObj.getBinaryObjects(), ownedByOtherObject);
            // Set the sort file size to the size of the original file
            doc.setFilesizeSort(getFilesize(datastreams));

            // Add list of derivatives associated from the representative file
            addDerivatives(datastreams, fileObj.getPid(), ownedByOtherObject);
        } else {
            // Add list of derivatives associated with the object
            addDerivatives(datastreams, contentObj.getPid(), false);
        }

        // Add in metadata datastreams
        addDatastreams(datastreams, contentObj.listMetadata(), false);

        doc.setFilesizeTotal(getFilesizeTotal(datastreams));
        doc.setDatastream(getDatastreamStrings(datastreams));
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
     * Adds a list of Datastream objects from the provided list of binaries.
     * If the datastreams are being recorded on an object  other than their owning
     * file object, the pid of the owning file object is recorded
     *
     * @param binList list of binaries
     * @param ownedByOtherObject
     */
    private void addDatastreams(List<Datastream> dsList, List<BinaryObject> binList, boolean ownedByOtherObject) {
        binList.stream().forEach(binary -> {
                Resource binaryResc = binary.getResource();

                String name = binaryResc.getURI();
                name = name.substring(name.lastIndexOf('/') + 1);

                String mimetype = binaryResc.getProperty(Ebucore.hasMimeType).getString();
                long filesize = binaryResc.getProperty(Premis.hasSize).getLong();
                // Making assumption that there is only one checksum
                String checksum = getFirstChecksum(binaryResc);

                String filename = binaryResc.getProperty(Ebucore.filename).getString();
                int extensionIndex = filename.lastIndexOf('.');
                String extension = extensionIndex == -1 ? "" : filename.substring(extensionIndex + 1);

                String owner = ownedByOtherObject ? binary.getPid().getId() : null;

                dsList.add(new Datastream(owner, name, filesize, mimetype, filename, extension, checksum));
            });
    }

    private String getFirstChecksum(Resource resc) {
        Statement prop = resc.getProperty(Premis.hasMessageDigest);
        if (prop == null) {
            return null;
        }
        return prop.getResource().getURI();
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
            .mapToLong(Datastream::getFilesize)
            .sum();
    }

    private long getFilesize(List<Datastream> datastreams) throws IndexingException {
        Optional<Datastream> original = datastreams.stream()
                .filter(ds -> ORIGINAL_FILE.getId().equals(ds.getName()))
                .findFirst();

        if (!original.isPresent()) {
            throw new IndexingException("File object in invalid state, cannot find original file binary");
        }

        return original.get().getFilesize();
    }

    private void addDerivatives(List<Datastream> dsList, PID pid, boolean ownedByOtherObject) {
        derivativeService.getDerivatives(pid).stream()
            .forEach(deriv -> {
                String owner = (ownedByOtherObject ? pid.getId() : null);

                DatastreamType type = deriv.getType();
                String name = type.getId();
                String mimetype = type.getMimetype();
                String extension = type.getExtension();

                File derivFile = deriv.getFile();
                Long filesize = derivFile.length();
                String filename = derivFile.getName();

                dsList.add(new Datastream(owner, name, filesize, mimetype, filename, extension, null));
            });
    }

    /**
     * @param derivativeService the derivativeService to set
     */
    public void setDerivativeService(DerivativeService derivativeService) {
        this.derivativeService = derivativeService;
    }
}
