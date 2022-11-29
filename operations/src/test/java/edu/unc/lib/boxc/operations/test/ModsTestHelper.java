package edu.unc.lib.boxc.operations.test;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.operations.jms.exportxml.BulkXMLConstants;

/**
 * @author bbpennel
 *
 */
public class ModsTestHelper {

    private ModsTestHelper() {
    }

    public static File writeToFile(Document doc) throws Exception {
        File xmlFile = File.createTempFile("doc", ".xml");
        copyInputStreamToFile(documentToInputStream(doc), xmlFile);
        return xmlFile;
    }

    public static InputStream documentToInputStream(Document doc) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        new XMLOutputter().output(doc, outStream);
        return new ByteArrayInputStream(outStream.toByteArray());
    }

    public static Document makeUpdateDocument() {
        Document doc = new Document();
        doc.addContent(new Element(BulkXMLConstants.BULK_MD_TAG));
        return doc;
    }

    public static Element addObjectUpdate(Document doc, PID pid, String lastModified) {
        Element objEl = addObject(doc, pid);
        Element dsEl = new Element(BulkXMLConstants.DATASTREAM_TAG);
        dsEl.setAttribute(BulkXMLConstants.TYPE_ATTR, DatastreamType.MD_DESCRIPTIVE.getId());
        dsEl.setAttribute(BulkXMLConstants.OPERATION_ATTR, BulkXMLConstants.OPER_UPDATE_ATTR);
        if (lastModified != null) {
            dsEl.setAttribute(BulkXMLConstants.MODIFIED_ATTR, lastModified);
        }
        objEl.addContent(dsEl);

        return dsEl;
    }

    public static Element addObject(Document doc, PID pid) {
        Element objEl = new Element(BulkXMLConstants.OBJECT_TAG);
        if (pid != null) {
            objEl.setAttribute(BulkXMLConstants.PID_ATTR, pid.getId());
        }
        doc.getRootElement().addContent(objEl);
        return objEl;
    }

    public static Element modsWithTitleAndDate(String title, String dateCreated) {
        Element modsEl = new Element("mods", MODS_V3_NS)
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS).setText(title)));
        if (dateCreated != null) {
            modsEl.addContent(new Element("originInfo", MODS_V3_NS)
                    .addContent(new Element("dateCreated", MODS_V3_NS)
                            .setAttribute("encoding", "iso8601")
                            .setAttribute("keyDate", "yes")
                            .setText(dateCreated)));
        }

        return modsEl;
    }
}
