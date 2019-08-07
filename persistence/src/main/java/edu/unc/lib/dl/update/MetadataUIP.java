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
package edu.unc.lib.dl.update;

import static edu.unc.lib.dl.util.AtomPubMetadataParserUtil.ATOM_DC_DATASTREAM;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.FileSystemException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

/**
 *
 * @author bbpennel
 *
 */
public class MetadataUIP extends FedoraObjectUIP {
    private static Logger log = LoggerFactory.getLogger(MetadataUIP.class);

    public MetadataUIP(PID pid, String user, UpdateOperation operation) {
        super(pid, user, operation);
        incomingData = new HashMap<String,Element>();
        originalData = new HashMap<String,Element>();
        modifiedData = new HashMap<String,Element>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Element> getIncomingData() {
        return (Map<String, Element>) incomingData;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Element> getOriginalData() {
        return (Map<String, Element>) originalData;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Element> getModifiedData() {
        return (Map<String, Element>) modifiedData;
    }

    @Override
    public String getMimetype(String key) {
        return "text/xml";
    }

    @Override
    public void storeOriginalDatastreams(AccessClient accessClient) throws UIPException {
        if (incomingData == null) {
            return;
        }
        SAXBuilder builder = new SAXBuilder();
        for (String datastream: incomingData.keySet()) {
            log.debug("Retrieving original document for " + datastream);
            // Only attempt to retrieve known datastreams
            if (Datastream.getDatastream(datastream) == null && !ATOM_DC_DATASTREAM.equals(datastream)) {
                log.debug("Datastream " + datastream + " was not a known datastream, skipping");
                continue;
            }
            ByteArrayInputStream inputStream = null;
            try {
                MIMETypedStream dsStream = accessClient.getDatastreamDissemination(pid, datastream, null);
                if (dsStream != null) {
                    inputStream = new ByteArrayInputStream(dsStream.getStream());
                    Document dsDocument = builder.build(inputStream);
                    Element rootElement = dsDocument.detachRootElement();
                    this.getOriginalData().put(datastream, rootElement);
                }
            } catch (NotFoundException e) {
                //Datastream wasn't found, therefore it doesn't exist and no original should be added
                log.debug("Datastream " + datastream + " was not found for pid " + pid);
            } catch (FedoraException e) {
                if (e instanceof FileSystemException || e instanceof AuthorizationException) {
                    throw new UIPException("Exception occurred while attempting to store datastream " + datastream
                            + " for " + pid.getPid(), e);
                }
                // Fedora isn't correctly identifying NotFoundExceptions in
                // 3.6.2's soap client, so identify it by process of elimination
            } catch (Exception e) {
                throw new UIPException("Exception occurred while attempting to store datastream " + datastream
                        + " for " + pid.getPid(), e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        throw new UIPException("Exception occurred while attempting to store datastream " + datastream
                                + " for " + pid.getPid(), e);
                    }
                }
            }
        }

    }

    /**
     * Generates temporary files for each metadata datastream and returns a hash of them by datastream
     */
    @Override
    public Map<String, File> getModifiedFiles() {
        Map<String, File> modifiedFiles = new HashMap<String, File>();
        for (Entry<String, ?> modified : modifiedData.entrySet()) {
            Element modifiedElement = (Element)modified.getValue();
            try {
                File temp = ClientUtils.writeXMLToTempFile(modifiedElement);
                modifiedFiles.put(modified.getKey(), temp);
            } catch (IOException e) {
                log.error("Failed to create temp file", e);
            }
        }
        return modifiedFiles;
    }
}
