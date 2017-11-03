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
package edu.unc.lib.dl.fcrepo4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Test;

import edu.unc.lib.dl.event.FilePremisLogger;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 *
 * @author bbpennel
 *
 */
public class DepositRecordIT extends AbstractFedoraIT {

    @Test
    public void createDepositRecordTest() throws Exception {

        Model model = getDepositRecordModel();

        DepositRecord record = repoObjFactory.createDepositRecord(model);

        assertNotNull(record);

        assertTrue(record.getTypes().contains(Cdr.DepositRecord.getURI()));
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void getInvalidDepositRecord() throws Exception {
        PID pid = pidMinter.mintContentPid();
        // Create a dummy non-depositRecord object
        client.put(pid.getRepositoryUri()).perform().close();

        // Try (and fail) to retrieve it as a deposit record
        repoObjLoader.getDepositRecord(pid);
    }

    @Test
    public void getDepositRecord() throws Exception {
        Model model = getDepositRecordModel();

        PID pid = repoObjFactory.createDepositRecord(model).getPid();

        DepositRecord record = repoObjLoader.getDepositRecord(pid);

        assertTrue(record.getTypes().contains(Cdr.DepositRecord.getURI()));
    }

    @Test
    public void addManifestsTest() throws Exception {

        Model model = getDepositRecordModel();

        DepositRecord record = repoObjFactory.createDepositRecord(model);

        String bodyString1 = "Manifest info";
        String filename1 = "manifest1.txt";
        String mimetype1 = "text/plain";
        InputStream contentStream1 = new ByteArrayInputStream(bodyString1.getBytes());
        BinaryObject manifest1 = record.addManifest(contentStream1, filename1, mimetype1);

        assertNotNull(manifest1);
        assertEquals(filename1, manifest1.getFilename());
        assertEquals(mimetype1, manifest1.getMimetype());

        String bodyString2 = "Second manifest";
        String filename2 = "manifest2.txt";
        String mimetype2 = "text/plain";
        InputStream contentStream2 = new ByteArrayInputStream(bodyString2.getBytes());
        BinaryObject manifest2 = record.addManifest(contentStream2, filename2, mimetype2);

        assertNotNull(manifest2);

        // Verify that listing returns all the expected manifests
        Collection<PID> manifestPids = record.listManifests();
        assertEquals("Incorrect number of manifests retrieved", 2, manifestPids.size());

        assertTrue("Manifest1 was not listed", manifestPids.contains(manifest1.getPid()));
        assertTrue("Manifest2 was not listed", manifestPids.contains(manifest2.getPid()));

        String respString1 = new BufferedReader(new InputStreamReader(manifest1.getBinaryStream()))
                .lines().collect(Collectors.joining("\n"));
        assertEquals("Manifest content did not match submitted value", bodyString1, respString1);

        // Verify that retrieving the manifest returns the correct object
        BinaryObject gotManifest2 = record.getManifest(manifest2.getPid());
        assertNotNull("Get manifest did not return", gotManifest2);
        assertEquals(filename2, gotManifest2.getFilename());
        assertEquals(mimetype2, gotManifest2.getMimetype());

        String respString2 = new BufferedReader(new InputStreamReader(manifest2.getBinaryStream()))
                .lines().collect(Collectors.joining("\n"));
        assertEquals("Manifest content did not match submitted value", bodyString2, respString2);
    }

    public void addPremisEventsTest() throws Exception {
        PID pid = pidMinter.mintDepositRecordPid();
        Model model = getDepositRecordModel();

        String details = "Event details";
        // Prep the events prior to ingest
        PremisLogger logger = new FilePremisLogger(pid, null, pidMinter, repoObjLoader, repoObjFactory, driver);
        logger.buildEvent(Premis.Ingestion)
                .addAuthorizingAgent(SoftwareAgent.depositService.toString())
                .addEventDetail("Event details")
                .write();
        logger.buildEvent(Premis.VirusCheck)
                .addSoftwareAgent(SoftwareAgent.clamav.toString())
                .write();

        // Push the events out to repository
        DepositRecord record = repoObjFactory.createDepositRecord(pid, model)
            .addPremisEvents(logger.getEvents());

        // Retrieve all the events added to this object
        List<PremisEventObject> events = record.getPremisLog().getEvents();
        Collections.sort(events);

        // Verify that they were added correctly
        Resource ingestEvent = events.get(0).getResource();
        assertTrue(ingestEvent.hasProperty(Premis.hasEventType, Premis.VirusCheck));
        assertEquals(details, ingestEvent.getProperty(Premis.hasEventDetail));

        // Check that the second event has the right type
        assertTrue(events.get(1).getResource().hasProperty(Premis.hasEventType, Premis.Ingestion));
    }

    @Test
    public void addObjectsTest() throws Exception {
        Model model = getDepositRecordModel();
        DepositRecord record = repoObjFactory.createDepositRecord(model);

        URI obj1Uri;
        URI obj2Uri;
        try (FcrepoResponse response = client.post(URI.create(baseAddress)).perform()) {
            obj1Uri = response.getLocation();
        }
        try (FcrepoResponse response = client.post(URI.create(baseAddress)).perform()) {
            obj2Uri = response.getLocation();
        }
        Resource res1 = model.createResource(obj1Uri.toString());
        Resource res2 = model.createResource(obj2Uri.toString());

        List<Resource> depositedObjs = new ArrayList<>();
        depositedObjs.add(res1);
        depositedObjs.add(res2);

        record.addIngestedObjects(depositedObjs);

        assertTrue(record.listDepositedObjects().size() == 2);
    }

    private Model getDepositRecordModel() {
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource("");
        resc.addProperty(RDF.type, Cdr.DepositRecord);

        return model;
    }
}
