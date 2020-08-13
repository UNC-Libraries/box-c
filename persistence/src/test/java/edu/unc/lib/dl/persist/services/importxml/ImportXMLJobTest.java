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
package edu.unc.lib.dl.persist.services.importxml;

import static edu.unc.lib.dl.persist.services.importxml.XMLImportTestHelper.addObject;
import static edu.unc.lib.dl.persist.services.importxml.XMLImportTestHelper.addObjectUpdate;
import static edu.unc.lib.dl.persist.services.importxml.XMLImportTestHelper.makeUpdateDocument;
import static edu.unc.lib.dl.persist.services.importxml.XMLImportTestHelper.modsWithTitleAndDate;
import static edu.unc.lib.dl.persist.services.importxml.XMLImportTestHelper.writeToFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.mail.internet.MimeMessage;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.samskivert.mustache.Template;

import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.persist.api.transfer.MultiDestinationTransferSession;
import edu.unc.lib.dl.persist.services.edit.UpdateDescriptionService;

/**
 *
 * @author harring
 *
 */
public class ImportXMLJobTest {

    private final static String ORIGINAL_TITLE = "Work Test";
    private final static String UPDATED_TITLE = "Updated Work Title";
    private final static String ORIGINAL_DATE = "2017-10-09";

    private static final String OBJ1_ID = "ae0091e0-192d-46f9-a8ad-8b0dc82f33ad";
    private static final String OBJ2_ID = "b75e416f-0ca8-4138-94ca-0a99bbd8e710";
    private static final String OBJ3_ID = "43dcb37a-27fc-425b-9c00-76cee952507c";

    private ImportXMLJob job;

    @Mock
    private AgentPrincipals agent;
    @Mock
    private Template completeTemplate;
    @Mock
    private Template failedTemplate;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private UpdateDescriptionService updateService;
    @Mock
    private BinaryTransferService transferService;
    @Mock
    private MultiDestinationTransferSession multiDestSession;
    @Mock
    private BinaryTransferSession singleDestSession;
    @Mock
    private StorageLocationManager locationManager;
    @Mock
    private StorageLocation storageLocation;

    @Mock
    private MimeMessage msg;
    @Mock
    private MimeMessageHelper msgHelper;
    @Mock
    private XMLEventReader xmlReader;
    @Mock
    private XMLEvent xmlEvent;
    @Mock
    private StartElement startEl;
    @Mock
    private QName qName;

    @Captor
    private ArgumentCaptor<String> subjectCaptor;
    @Captor
    private ArgumentCaptor<String> emailCaptor;
    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void init() throws Exception {
        initMocks(this);

        when(mailSender.createMimeMessage()).thenReturn(msg);

        when(transferService.getSession()).thenReturn(multiDestSession);
        when(multiDestSession.forDestination(storageLocation)).thenReturn(singleDestSession);
        when(locationManager.getStorageLocation(any(PID.class))).thenReturn(storageLocation);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void emptyFileTest() throws Exception {
        File importFile = tmpFolder.newFile();
        setupJob(importFile);
        job.run();

        verify(mailSender).send(msg);
        verify(failedTemplate).execute(mapCaptor.capture());

        Map<String, Object> dataMap = mapCaptor.getValue();
        assertEquals(importFile.getAbsolutePath(), dataMap.get("fileName"));
        assertEquals(1, dataMap.get("problemCount"));
        Set<Entry<String, String>> problems = (Set<Entry<String, String>>) dataMap.get("problems");
        Entry<String, String> problem = problems.iterator().next();
        assertEquals("The import file contains XML errors", problem.getValue());

        verify(msg).setSubject(subjectCaptor.capture());
        assertEquals("DCR Metadata update failed", subjectCaptor.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fileNotFoundTest() throws Exception {
        File importFile = new File("doesnotexist.xml");
        setupJob(importFile);
        job.run();

        verify(mailSender).send(msg);
        verify(failedTemplate).execute(mapCaptor.capture());

        Map<String, Object> dataMap = mapCaptor.getValue();
        assertEquals(1, dataMap.get("problemCount"));
        Set<Entry<String, String>> problems = (Set<Entry<String, String>>) dataMap.get("problems");
        Entry<String, String> problem = problems.iterator().next();
        assertTrue("Failure message did not match excepted value, was: " + problem.getValue(),
                problem.getValue().contains("Failed to read import file"));

        verify(msg).setSubject(subjectCaptor.capture());
        assertEquals("DCR Metadata update failed", subjectCaptor.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void incorrectOpeningTagTest() throws Exception {
        Document updateDoc = new Document();
        updateDoc.addContent(new Element("wrongRoot"));
        addObjectUpdate(updateDoc, PIDs.get("ae0091e0-192d-46f9-a8ad-8b0dc82f33ad"), null)
            .addContent(modsWithTitleAndDate("Updated Work Test", null));
        File importFile = writeToFile(updateDoc);

        setupJob(importFile);

        when(xmlReader.nextEvent()).thenReturn(xmlEvent);
        when(xmlEvent.isStartElement()).thenReturn(true);
        when(xmlEvent.asStartElement()).thenReturn(startEl);
        when(startEl.getName()).thenReturn(qName);
        when(qName.getLocalPart()).thenReturn("not bulk md");

        job.run();

        verify(mailSender).send(msg);
        verify(failedTemplate).execute(mapCaptor.capture());

        Map<String, Object> dataMap = mapCaptor.getValue();
        assertEquals(1, dataMap.get("problemCount"));
        Set<Entry<String, String>> problems = (Set<Entry<String, String>>) dataMap.get("problems");
        Entry<String, String> problem = problems.iterator().next();
        assertEquals("File is not a bulk-metadata-update doc", problem.getValue());

        verify(msg).setSubject(subjectCaptor.capture());
        assertEquals("DCR Metadata update failed", subjectCaptor.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void successfulJobTest() throws Exception {
        File tempImportFile = createTempImportFile();
        setupJob(tempImportFile);
        when(completeTemplate.execute(anyObject())).thenReturn("success email text");
        job.run();

        verify(mailSender).send(msg);
        verify(completeTemplate).execute(mapCaptor.capture());
        Map<String, Object> dataMap = mapCaptor.getValue();
        assertEquals(tempImportFile.getPath(), dataMap.get("fileName"));
        assertNull(dataMap.get("failed"));
        assertNull(dataMap.get("failedCount"));
        assertNull(dataMap.get("issues"));

        List<String> updated = (List<String>) dataMap.get("updated");
        assertEquals(2, updated.size());
        assertTrue(updated.contains(OBJ1_ID));
        assertTrue(updated.contains(OBJ2_ID));
        assertEquals(2, dataMap.get("updatedCount"));

        verify(msg).setSubject(subjectCaptor.capture());
        assertTrue(subjectCaptor.getValue().startsWith("DCR Metadata update completed"));
    }

    private File createTempImportFile() throws Exception {
        Document updateDoc = makeUpdateDocument();
        addObjectUpdate(updateDoc, PIDs.get(OBJ1_ID), null)
            .addContent(modsWithTitleAndDate(ORIGINAL_TITLE, ORIGINAL_DATE));
        addObjectUpdate(updateDoc, PIDs.get(OBJ2_ID), "2017-10-18T12:29:53.396Z")
            .addContent(modsWithTitleAndDate(UPDATED_TITLE, null));
        addObject(updateDoc, PIDs.get(OBJ3_ID));

        return writeToFile(updateDoc);
    }

    private void setupJob(File importFile) {
        String userEmail = "user@email.com";
        ImportXMLRequest request = new ImportXMLRequest(userEmail, agent, importFile);
        job = new ImportXMLJob(request);
        job.setCompleteTemplate(completeTemplate);
        job.setFailedTemplate(failedTemplate);
        job.setFromAddress("admin@example.com");
        job.setMailSender(mailSender);
        job.setUpdateService(updateService);
        job.setMimeMessage(msg);
        job.setMessageHelper(msgHelper);
        job.setLocationManager(locationManager);
        job.setTransferService(transferService);
    }

}
