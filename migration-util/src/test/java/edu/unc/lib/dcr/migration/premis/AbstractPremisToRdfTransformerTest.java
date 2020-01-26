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
package edu.unc.lib.dcr.migration.premis;

import static edu.unc.lib.dl.util.DateTimeUtil.formatDateToUTC;
import static edu.unc.lib.dl.util.DateTimeUtil.parseUTCToDate;
import static edu.unc.lib.dl.util.RDFModelUtil.createModel;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.PREMIS_V2_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.XSI_NS;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jgroups.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import edu.unc.lib.dl.event.FilePremisLogger;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;

/**
 * @author bbpennel
 *
 */
public abstract class AbstractPremisToRdfTransformerTest {

    protected static final String EVENT_DATE = "2015-10-19T17:06:22";
    protected static final String EVENT_DATE_UTC = formatDateToUTC(parseUTCToDate(EVENT_DATE));

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    protected File logFile;

    protected PID objPid;

    protected Document premisDoc;

    protected PremisLogger premisLogger;
    protected RepositoryPIDMinter pidMinter;

    @Before
    public void setupParent() throws Exception {
        pidMinter = new RepositoryPIDMinter();

        objPid = makePid();
        logFile = tmpFolder.newFile(objPid.getId() + ".nt");
        premisDoc = createPremisDoc(objPid);
        premisLogger = new FilePremisLogger(objPid, logFile, pidMinter);
    }

    protected Model deserializeLogFile(File logFile) throws IOException {
        return createModel(new FileInputStream(logFile), "N-TRIPLE");
    }

    protected List<Resource> listEventResources(PID pid, Model model) {
        Resource objResc = model.getResource(objPid.getRepositoryPath());

        return objResc.listProperties(Premis.hasEvent).toList().stream()
                .map(Statement::getResource)
                .collect(toList());
    }

    protected PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    protected Document createPremisDoc(PID pid) {
        Document doc = new Document();
        doc.addContent(new Element("premis", PREMIS_V2_NS)
                .addContent(new Element("object", PREMIS_V2_NS)
                        .setAttribute("type", "representation", XSI_NS)
                        .addContent(new Element("objectIdentifier", PREMIS_V2_NS)
                                .addContent(new Element("objectIdentifierType", PREMIS_V2_NS).setText("PID"))
                                .addContent(new Element("objectIdentifierValue", PREMIS_V2_NS)
                                        .setText("uuid:" + pid.getId())))));
        return doc;
    }

    protected Resource getResourceByEventDate(List<Resource> rescs, String eventDate) {
        return rescs.stream().filter(r -> eventDate.equals(r.getProperty(Premis.hasEventDateTime).getString()))
                .findFirst().get();
    }

    protected void assertEventType(Resource expectedType, Resource eventResc) {
        assertEquals("Event type did not match expected value",
                expectedType, eventResc.getPropertyResourceValue(Premis.hasEventType));
    }

    protected void assertEventDetail(String expected, Resource eventResc) {
        List<String> details = eventResc.listProperties(Premis.hasEventDetail).toList().stream()
                .map(Statement::getString).collect(toList());
        if (details.contains(expected)) {
            return;
        }
        fail("Event detail message did not match expected value " + expected
                + ", the following values were present: " + String.join(",", details));
    }

    protected void assertEventDateTime(String expected, Resource eventResc) {
        assertEquals("Event date time did not match expected value",
                expected, eventResc.getProperty(Premis.hasEventDateTime).getString());
    }
}
