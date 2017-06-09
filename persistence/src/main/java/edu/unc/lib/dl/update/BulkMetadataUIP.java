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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.PremisEventLogger;

/**
 * Bulk metadata update information package, stores information related to the entire import and retrieves
 * information for individual object datastream updates.
 *
 * @author bbpennel
 * @date Jul 30, 2015
 */
public class BulkMetadataUIP extends UIPImpl {
    private static final Logger log = LoggerFactory.getLogger(BulkMetadataUIP.class);

    private PID currentPid;
    private int updateCount = 0;
    private int objectCount = 0;

    private final XMLOutputFactory xmlOutput;
    private XMLEventReader xmlReader;
    private DocumentState state = DocumentState.ROOT;
    private final String emailAddress;
    private final AccessGroupSet groups;
    private final File importFile;
    private final String originalFilename;
    private final boolean existingUpdate;

    private final static String BULK_MD_TAG = "bulkMetadata";
    private final static String OBJECT_TAG = "object";
    private final static String UPDATE_TAG = "update";
    private final static String MODS_TYPE = "MODS";
    private final static QName pidAttribute = new QName("pid");
    private final static QName lastModifiedAttribute = new QName("lastModified");
    private final static QName typeAttribute = new QName("type");

    private enum DocumentState {
        ROOT, IN_BULK, IN_OBJECT, IN_CONTENT;
    }

    public BulkMetadataUIP(String pid, String emailAddress, String user, AccessGroupSet groups,
            File importFile, String originalFilename) throws UIPException {
        super(new PID(pid == null ? UUID.randomUUID().toString() : pid), user, UpdateOperation.REPLACE);

        existingUpdate = pid != null;

        this.emailAddress = emailAddress;
        this.groups = groups;
        this.importFile = importFile;
        this.originalFilename = originalFilename;

        xmlOutput = XMLOutputFactory.newInstance();
        initializeXMLReader();
    }

    private void initializeXMLReader() throws UIPException {
        XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
        try {
            xmlReader = xmlFactory.createXMLEventReader(new FileInputStream(importFile));
        } catch (FileNotFoundException | XMLStreamException e) {
            throw new UIPException("Failed to read metadata update package for " + user, e);
        }
    }

    public BulkMetadataDatastreamUIP getNextUpdate()
            throws UpdateException, JDOMException, XMLStreamException {
        return seekNextUpdate(null, null);
    }

    public BulkMetadataDatastreamUIP seekNextUpdate(PID resumePid, String resumeDs)
            throws UpdateException, JDOMException, XMLStreamException {
        QName contentOpening = null;
        long countOpenings = 0;
        XMLEventWriter xmlWriter = null;
        StringWriter contentWriter = null;

        String currentDs = null;
        String lastModified = null;

        boolean resumeMode = resumePid != null;
        boolean foundResumptionPoint = false;

        try {
            while (xmlReader.hasNext()) {
                XMLEvent e = xmlReader.nextEvent();

                switch (state) {
                    case ROOT:
                        if (e.isStartElement()) {
                            StartElement element = e.asStartElement();
                            // Make sure that this document begins with bulk md tag
                            if (element.getName().getLocalPart().equals(BULK_MD_TAG)) {
                                state = DocumentState.IN_BULK;
                            }
                        }
                        break;
                    case IN_BULK:
                        if (e.isStartElement()) {
                            StartElement element = e.asStartElement();
                            // Found an opening object tag, capture its PID
                            if (element.getName().getLocalPart().equals(OBJECT_TAG)) {
                                Attribute pid = element.getAttributeByName(pidAttribute);

                                if (pid != null) {
                                    currentPid = new PID(pid.getValue());
                                    objectCount++;
                                }
                                state = DocumentState.IN_OBJECT;
                            }
                        }
                        break;
                    case IN_OBJECT:
                        if (e.isStartElement()) {
                            StartElement element = e.asStartElement();
                            // Found start of update, extract the datastream
                            if (element.getName().getLocalPart().equals(UPDATE_TAG)) {
                                // Get last modified date if available
                                Attribute lastModifiedAttr = element.getAttributeByName(lastModifiedAttribute);
                                lastModified = lastModifiedAttr == null ? null : lastModifiedAttr.getValue();

                                Attribute typeAttr = element.getAttributeByName(typeAttribute);
                                if (typeAttr == null) {
                                    throw new UpdateException("Invalid import data, missing type attribute"
                                            + " on update of " + currentPid);
                                }
                                if (MODS_TYPE.equals(typeAttr.getValue())) {
                                    currentDs = Datastream.MD_DESCRIPTIVE.toString();
                                } else {
                                    throw new UpdateException("Invalid import data, unsupport type in update tag "
                                            + currentPid);
                                }

                                foundResumptionPoint = resumeMode
                                        && currentPid.equals(resumePid) && currentDs.equals(resumeDs);

                                state = DocumentState.IN_CONTENT;
                                if (!resumeMode) {
                                    contentWriter = new StringWriter();
                                    xmlWriter = xmlOutput.createXMLEventWriter(contentWriter);
                                }
                            }
                        } else if (e.isEndElement()) {
                            // Closing object tag
                            state = DocumentState.IN_BULK;
                        }
                        break;
                    case IN_CONTENT:
                        // Record the name of the content opening tag so we can tell when it closes
                        if (e.isStartElement()) {
                            if (contentOpening == null) {
                                contentOpening = e.asStartElement().getName();
                                countOpenings = 1;
                            } else if (contentOpening.equals(e.asStartElement().getName())) {
                                // Count the number of openings just in case there are nested records
                                countOpenings++;
                            }
                        }
                        // Subtract from count of opening tags for root element
                        if (e.isEndElement() && contentOpening.equals(e.asEndElement().getName())) {
                            countOpenings--;
                        }

                        // Finished with opening tags and the update tag is ending, done with content.
                        if (countOpenings == 0 && e.isEndElement() && UPDATE_TAG.equals(
                                e.asEndElement().getName().getLocalPart())) {
                            state = DocumentState.IN_OBJECT;
                            // Increment the number of updates retrieved
                            updateCount++;
                            if (!resumeMode) {
                                xmlWriter.close();
                                xmlWriter = null;
                                Document dsDoc = new SAXBuilder()
                                        .build(new ByteArrayInputStream(contentWriter.toString().getBytes()));
                                return new BulkMetadataDatastreamUIP(currentPid, user, UpdateOperation.REPLACE,
                                        currentDs, lastModified, dsDoc);
                            } else if (foundResumptionPoint) {
                                return null;
                            }
                        } else {
                            if (!resumeMode) {
                                // Store all of the content from the incoming document
                                xmlWriter.add(e);
                            }
                        }

                        break;
                }
            }
        } catch (IOException e) {
            throw new UpdateException("Could not parse content for " + currentPid, e);
        } finally {
            if (xmlWriter != null) {
                xmlWriter.close();
            }
        }

        return null;
    }

    public void close() {
        try {
            xmlReader.close();
        } catch (XMLStreamException e) {
            log.error("Failed to close XML Reader during CDR metadata update", e);
        }
    }

    public void reset() throws UIPException {
        close();
        initializeXMLReader();

        updateCount = 0;
        objectCount = 0;
    }

    /* (non-Javadoc)
     * @see edu.unc.lib.dl.update.UpdateInformationPackage#getEventLogger()
     */
    @Override
    public PremisEventLogger getEventLogger() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public AccessGroupSet getGroups() {
        return groups;
    }

    public File getImportFile() {
        return importFile;
    }

    public boolean isExistingUpdate() {
        return existingUpdate;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public int getObjectCount() {
        return objectCount;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public PID getCurrentPid() {
        return currentPid;
    }
}
