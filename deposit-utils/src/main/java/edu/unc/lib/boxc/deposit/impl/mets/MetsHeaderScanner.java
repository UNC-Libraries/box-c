package edu.unc.lib.boxc.deposit.impl.mets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.xml.NamespaceConstants;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * An quick SAX extractor of METS header information.
 *
 * @author count0
 *
 */
public class MetsHeaderScanner extends DefaultHandler {
    Logger log = LoggerFactory.getLogger(MetsHeaderScanner.class);

    PID objID = null;
    String label = null;
    String createDate = null;
    String lastModDate = null;
    String profile = null;
    String type = null;
    String id = null;
    List<String> names = new ArrayList<>();
    StringBuilder nameBuffer = null;

    public List<String> getNames() {
        return names;
    }

    String metsURI = NamespaceConstants.METS_URI;

    public PID getObjID() {
        return objID;
    }

    public String getLabel() {
        return label;
    }

    public String getCreateDate() {
        return createDate;
    }

    public String getLastModDate() {
        return lastModDate;
    }

    public String getProfile() {
        return profile;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attr) throws SAXException {
        if (!NamespaceConstants.METS_URI.equals(uri)) {
            return;
        }

        if (localName.equals("mets")) {
            for (int i = 0; i < attr.getLength(); i++) {
                String n = attr.getLocalName(i);
                if (n.equals("OBJID")) {
                    objID = PIDs.get(attr.getValue(i));
                } else if (n.equals("ID")) {
                    id = attr.getValue(i);
                } else if (n.equals("LABEL")) {
                    label = attr.getValue(i);
                } else if (n.equals("PROFILE")) {
                    profile = attr.getValue(i);
                } else if (n.equals("TYPE")) {
                    type = attr.getValue(i);
                }
            }
        } else if (localName.equals("metsHdr")) {
            for (int i = 0; i < attr.getLength(); i++) {
                String n = attr.getLocalName(i);
                if (n.equals("CREATEDATE")) {
                    createDate = attr.getValue(i);
                } else if (n.equals("LASTMODDATE")) {
                    lastModDate = attr.getValue(i);
                }
            }
        } else if (localName.equals("name")) {
            nameBuffer = new StringBuilder();
        }
        super.startElement(uri, localName, qName, attr);
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (nameBuffer != null) {
            nameBuffer.append(ch, start, length);
        }
        super.characters(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (!NamespaceConstants.METS_URI.equals(uri)) {
            return;
        }

        if (localName.equals("name")) {
            if (nameBuffer != null) {
                names.add(nameBuffer.toString());
                nameBuffer = null;
            }
        }
        super.endElement(uri, localName, qName);
    }

    public void scan(File f, String filename) throws Exception {
        @SuppressWarnings("resource")
        InputStream toParse = null;
        try {
            if (filename.endsWith(".zip")) {
                log.debug("scanning for METS within a zip file");
                @SuppressWarnings("resource")
                ZipArchiveInputStream zis = new ZipArchiveInputStream(
                        new FileInputStream(f));
                ArchiveEntry entry = null;
                while ((entry = zis.getNextZipEntry()) != null) {
                    if (!entry.isDirectory()) {
                        if (entry.getName().equals("METS.xml")
                                || entry.getName().equals("mets.xml")) {
                            log.debug("Found METS entry in ZIP: {}",
                                    entry.getName());
                            toParse = zis;
                            break;
                        }
                    }
                }
            } else {
                log.debug("scanning METS file");
                toParse = new FileInputStream(f);
            }
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://xml.org/sax/features/namespaces", true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            SAXParser saxParser = null;
            saxParser = factory.newSAXParser();
            saxParser.parse(toParse, this);
        } finally {
            if (toParse != null) {
                try {
                    toParse.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

}
