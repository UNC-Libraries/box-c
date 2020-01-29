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

import static edu.unc.lib.dcr.migration.premis.Premis2Constants.INITIATOR_ROLE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.METS_NORMAL_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VALIDATION_TYPE;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VIRUS_AGENT;
import static edu.unc.lib.dcr.migration.premis.Premis2Constants.VIRUS_CHECK_TYPE;
import static edu.unc.lib.dcr.migration.premis.PremisEventXMLHelpers.EVENT_DATE;
import static edu.unc.lib.dcr.migration.premis.PremisEventXMLHelpers.addAgent;
import static edu.unc.lib.dcr.migration.premis.PremisEventXMLHelpers.addEvent;
import static edu.unc.lib.dcr.migration.premis.PremisEventXMLHelpers.createPremisDoc;
import static edu.unc.lib.dcr.migration.premis.PremisEventXMLHelpers.serializeXMLFile;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import edu.unc.lib.dl.event.PremisLoggerFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 *
 */
public class TransformDepositPremisServiceTest {

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private Path outputPath;
    private Path originalLogsPath;
    private Path premisListPath;

    private RepositoryPIDMinter pidMinter;
    private PremisLoggerFactory premisLoggerFactory;

    private TransformDepositPremisService service;

    @Before
    public void setup() {
        pidMinter = new RepositoryPIDMinter();
        premisLoggerFactory = new PremisLoggerFactory();

        outputPath = tmpFolder.newFolder("output").toPath();
        originalLogsPath = tmpFolder.newFolder("originals").toPath();
        premisListPath = tmpFolder.newFile("premis_list.txt").toPath();

        service = new TransformDepositPremisService(premisListPath, outputPath);
    }

    @Test
    public void transformMultipleObjectEvents() throws Exception {
        PID pid1 = pidMinter.mintDepositRecordPid();
        Document premisDoc1 = createPremisDoc(pid1);
        String detail1 = "28 files scanned for viruses.";
        Element eventEl1 = addEvent(premisDoc1, VIRUS_CHECK_TYPE, detail1, EVENT_DATE);
        addInitiatorAgent(eventEl1, VIRUS_AGENT);

        serializeXMLFile(originalLogsPath, pid1, premisDoc1);

        PID pid2 = pidMinter.mintDepositRecordPid();
        Document premisDoc2 = createPremisDoc(pid2);
        String detail2 = "METS schema(s) validated";
        Element eventEl2 = addEvent(premisDoc2, VALIDATION_TYPE, detail2, EVENT_DATE);
        addInitiatorAgent(eventEl2, METS_NORMAL_AGENT);

        serializeXMLFile(originalLogsPath, pid2, premisDoc2);


    }

    private void addInitiatorAgent(Element eventEl, String agent) {
        addAgent(eventEl, "PID", INITIATOR_ROLE, agent);
    }

    private void buildPremisListFile() throws IOException {
        try (Stream<Path> walk = Files.walk(originalLogsPath)) {
            List<String> result = walk.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(toList());
        }
    }
}
