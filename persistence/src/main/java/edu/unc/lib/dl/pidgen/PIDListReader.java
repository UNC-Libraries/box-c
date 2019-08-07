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
package edu.unc.lib.dl.pidgen;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author count0
 *
 */
public class PIDListReader extends DefaultHandler {

    private static final Logger logger = LoggerFactory.getLogger(PIDListReader.class.getName());

    private List<PID> m_pids;
    private StringBuffer m_pidBuffer;
    private boolean m_inPID;

    public PIDListReader(InputStream xml) throws IOException {
        m_pids = new ArrayList<PID>();
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser parser = spf.newSAXParser();
            parser.parse(xml, this);
        } catch (Exception e) {
            logger.warn("Error parsing pidList", e);
            String msg = e.getMessage();
            if (msg == null) {
                msg = e.getClass().getName();
            }
            throw new IOException("Error parsing pidList: " + msg);
        }
    }

    public PID[] getPIDArray() {
        PID[] array = new PID[m_pids.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = m_pids.get(i);
        }
        return array;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes a) {
        if (localName.equals("pid")) {
            m_inPID = true;
            m_pidBuffer = new StringBuffer();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (m_inPID) {
            m_pidBuffer.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (localName.equals("pid")) {
            m_pids.add(new PID(m_pidBuffer.toString()));
            m_inPID = false;
        }
    }

}
