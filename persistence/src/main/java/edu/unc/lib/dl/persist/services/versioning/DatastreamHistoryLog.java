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
package edu.unc.lib.dl.persist.services.versioning;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.xml.SecureXMLFactory;

/**
 * Object representing the history of a datastream as a timestamped XML based log.
 *
 * @author bbpennel
 */
public class DatastreamHistoryLog {

    private static final String XML_TYPE_TEXT = "text/xml";
    private static final String XML_TYPE_APP = "application/xml";

    public static final String HISTORY_TAG = "datastreamHistory";
    public static final String VERSION_TAG = "version";
    public static final String ID_ATTR = "id";
    public static final String CONTENT_TYPE_ATTR = "contentType";
    public static final String CREATED_ATTR = "created";


    private Document historyDoc;
    private PID datastreamPid;

    /**
     * Instantiate a new history log for the specified datastream
     *
     * @param datastreamPid
     */
    public DatastreamHistoryLog(PID datastreamPid) {
        this.datastreamPid = datastreamPid;
        historyDoc = new Document();
        Element historyEl = new Element(HISTORY_TAG)
                .setAttribute(ID_ATTR, datastreamPid.getQualifiedId());
        historyDoc.addContent(historyEl);
    }

    /**
     * Instantiate from an existing history log
     *
     * @param logStream
     * @throws JDOMException
     * @throws IOException
     */
    public DatastreamHistoryLog(PID datastreamPid, InputStream logStream) {
        this.datastreamPid = datastreamPid;
        try {
            historyDoc = SecureXMLFactory.createSAXBuilder().build(logStream);
        } catch (JDOMException | IOException e) {
            throw new ServiceException("Failed to parse datastream history for " + datastreamPid.getQualifiedId(), e);
        }
    }

    /**
     * Add a version of the datastream to the history log
     *
     * @param content InputStream containing the content of the new datastream version
     * @param contentType content type of the datastream version
     * @param created timestamp the datastream version was created
     */
    public void addVersion(InputStream content, String contentType, Date created) {
        Element versionEl = new Element(VERSION_TAG)
                .setAttribute(CREATED_ATTR, DateTimeUtil.formatDateToUTC(created))
                .setAttribute(CONTENT_TYPE_ATTR, contentType);

        try {
            if (XML_TYPE_TEXT.equals(contentType) || XML_TYPE_APP.equals(contentType)) {
                Document contentDoc = SecureXMLFactory.createSAXBuilder().build(content);
                Element contentRoot = contentDoc.getRootElement();
                versionEl.addContent(contentRoot.detach());
            } else {
                versionEl.setText(IOUtils.toString(content, UTF_8));
            }
        } catch (JDOMException | IOException e) {
            throw new ServiceException("Failed to add new version of " + datastreamPid.getQualifiedId(), e);
        }

        historyDoc.getRootElement().addContent(versionEl);
    }

    /**
     * Serialize the history log into an inputstream
     *
     * @return the XML log as an InputStream
     * @throws IOException thrown if unable to serialize the log
     */
    public InputStream toInputStream() throws IOException {
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            new XMLOutputter(Format.getPrettyFormat()).output(historyDoc, outStream);
            return new ByteArrayInputStream(outStream.toByteArray());
        }
    }
}
