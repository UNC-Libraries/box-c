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
package edu.unc.lib.dl.fedora;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.Format;
import org.jdom2.output.SAXOutputter;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.xml.StandaloneDatastreamOutputFilter;

/**
 * 
 * @author bbpennel
 *
 */
@SuppressWarnings("deprecation")
public abstract class ClientUtils {
    private static final Log log = LogFactory.getLog(ClientUtils.class);

    public static Document parseXML(byte[] input) throws SAXException {
        Document result = null;
        SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);
        try {
            result = builder.build(new ByteArrayInputStream(input));
        } catch (JDOMException e) {
            throw new ServiceException("Unexpected error", e);
        } catch (IOException e) {
            throw new ServiceException("Unexpected error", e);

        }
        return result;
    }

    /**
     * Serializes the FOXML 1.1 JDOM document to a UTF-8 byte array. This method is responsible for assuring that the
     * FOXML output contains "standalone" datastreams with locally declared namespaces as required by Fedora ingest.
     *
     * @param doc
     *           a FOXML 1.1 JDOM document
     * @return a byte array serialization of the Document
     */
    public static byte[] serializeXML(Document doc) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer pw;
        try {
            pw = new OutputStreamWriter(baos, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ServiceException("UTF-8 character encoding support is required", e);
        }
        try {
            // Filtering SAX Events before serializing.
            // This ensures that any XML datastreams have locally
            // declared namespaces, which is a Fedora ingest
            // requirement.

            OutputFormat format = new OutputFormat("XML", "UTF-8", true);
            format.setIndent(2);
            format.setIndenting(true);
            format.setPreserveSpace(false);
            format.setLineWidth(200);
            XMLSerializer serializer = new XMLSerializer(pw, format);
            ContentHandler chs = serializer.asContentHandler();
            StandaloneDatastreamOutputFilter filter = new StandaloneDatastreamOutputFilter();
            filter.setContentHandler(chs);
            SAXOutputter sax = new SAXOutputter();
            sax.setContentHandler(filter);
            sax.output(doc);
            pw.flush();
        } catch (JDOMException e) {
            throw new ServiceException("Could not generate SAX events from JDOM.", e);
        } catch (IOException e) {
            throw new ServiceException("Could not obtain a content handler for the appropriate format and writer.", e);
        }
        byte[] result = baos.toByteArray();
        if (log.isDebugEnabled()) {
            try (
                    StringWriter sw = new StringWriter();
                    ByteArrayInputStream is = new ByteArrayInputStream(result);
                    ) {
                for (int f = is.read(); f != -1; f = is.read()) {
                    sw.write(f);
                }
                log.debug(sw.toString());
            } catch (IOException e) {
                throw new ServiceException(e);
            }
        }
        return result;
    }

    /**
     * Serializes a non-FOXML root XML element.  Does not attempt to attach local namespaces to sub-elements.
     * @param element
     * @return
     */
    public static byte[] serializeXML(Element element) {
        Format format = Format.getPrettyFormat();
        XMLOutputter outputter = new XMLOutputter(format);
        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Writer pw = new OutputStreamWriter(baos, "UTF-8");
                ) {
            outputter.output(element, pw);
            pw.flush();
            byte[] result = baos.toByteArray();
            return result;
        } catch (UnsupportedEncodingException e) {
            throw new ServiceException("UTF-8 character encoding support is required", e);
        } catch (IOException e) {
            log.error("Failed to serialize element", e);
        }
        return null;
    }

    /**
     * Serializes a JDOM element to a UTF-8 encoded file.  Does not enforce locally declared namespaces.
     * @param element a detached, standalone element
     * @return
     */
    public static File writeXMLToTempFile(Element element) throws IOException {
        Format format = Format.getPrettyFormat();
        XMLOutputter outputter = new XMLOutputter(format);
        File result = File.createTempFile("ClientUtils-", ".xml");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(result))) {
            outputter.output(element, writer);
            return result;
        }
    }

    /**
     * Serializes the FOXML 1.1 JDOM document to a UTF-8 byte array. This method is responsible for assuring that the
     * FOXML output contains "standalone" datastreams with locally declared namespaces as required by Fedora ingest.
     *
     * @param doc
     *           a FOXML 1.1 JDOM document
     * @return a byte array serialization of the Document
     */
    public static File writeXMLToTempFile(Document doc) {

        File result;
        try {
            result = File.createTempFile("ClientUtils-", ".xml");
        } catch (IOException e1) {
            throw new ServiceException("Could not create temp file.", e1);
        }
        try (
                FileOutputStream baos = new FileOutputStream(result);
                PrintWriter pw = new PrintWriter(baos);
                ) {
            // Filtering SAX Events before serializing.
            // This ensures that any XML datastreams have locally
            // declared namespaces, which is a Fedora ingest
            // requirement.
            OutputFormat format = new OutputFormat("XML", "UTF-8", true);
            format.setIndent(2);
            format.setIndenting(true);
            format.setPreserveSpace(false);
            format.setLineWidth(200);
            XMLSerializer serializer = new XMLSerializer(pw, format);
            ContentHandler chs = serializer.asContentHandler();
            StandaloneDatastreamOutputFilter filter = new StandaloneDatastreamOutputFilter();
            filter.setContentHandler(chs);
            SAXOutputter sax = new SAXOutputter();
            sax.setContentHandler(filter);
            sax.output(doc);
            return result;
        } catch (JDOMException e) {
            throw new ServiceException("Could not generate SAX events from JDOM.", e);
        } catch (IOException e) {
            throw new ServiceException("Could not obtain a content handler for the appropriate format and writer.", e);
        }
    }

}
