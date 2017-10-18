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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.annotation.Resource;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.ContentModelHelper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class MODSValidationUIPFilterTest extends Assert {
    private static Logger log = Logger.getLogger(MODSValidationUIPFilterTest.class);

    @Resource
    private SchematronValidator schematronValidator;
    private MODSValidationUIPFilter filter;

    @Before
    public void init() {
        initMocks(this);

        filter = new MODSValidationUIPFilter();

        filter.setSchematronValidator(schematronValidator);
    }

    @Test
    public void addMODSToObjectWithoutMODS() throws Exception {
        InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUpdateMODS.xml"));
        Abdera abdera = new Abdera();
        Parser parser = abdera.getParser();
        Document<Entry> entryDoc = parser.parse(entryPart);
        Entry entry = entryDoc.getRoot();

        AccessClient accessClient = mock(AccessClient.class);
        when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString())).thenReturn(null);

        PID pid = new PID("uuid:test");

        AtomPubMetadataUIP uip = new AtomPubMetadataUIP(pid, "testuser", UpdateOperation.ADD, entry);
        uip.storeOriginalDatastreams(accessClient);

        assertFalse(uip.getOriginalData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
        assertFalse(uip.getModifiedData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
        assertTrue(uip.getIncomingData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));

        filter.doFilter(uip);

        assertFalse(uip.getOriginalData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
        assertTrue(uip.getModifiedData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
        assertTrue(uip.getIncomingData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
    }

    @Test
    public void addMODSToObjectWithMODS() throws Exception {
        InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUpdateMODS.xml"));
        Abdera abdera = new Abdera();
        Parser parser = abdera.getParser();
        Document<Entry> entryDoc = parser.parse(entryPart);
        Entry entry = entryDoc.getRoot();

        AccessClient accessClient = mock(AccessClient.class);
        MIMETypedStream modsStream = new MIMETypedStream();
        File raf = new File("src/test/resources/testmods.xml");
        byte[] bytes = FileUtils.readFileToByteArray(raf);
        modsStream.setStream(bytes);
        modsStream.setMIMEType("text/xml");
        when(
                accessClient.getDatastreamDissemination(any(PID.class),
                        eq(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()), anyString())).thenReturn(modsStream);

        PID pid = new PID("uuid:test");

        AtomPubMetadataUIP uip = new AtomPubMetadataUIP(pid, "testuser", UpdateOperation.ADD, entry);
        uip.storeOriginalDatastreams(accessClient);

        assertTrue(uip.getOriginalData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
        assertFalse(uip.getModifiedData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
        assertTrue(uip.getIncomingData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));

        int originalChildrenCount = uip.getOriginalData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName())
                .getChildren().size();
        int incomingChildrenCount = uip.getIncomingData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName())
                .getChildren().size();

        filter.doFilter(uip);

        assertEquals(originalChildrenCount,
                uip.getOriginalData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()).getChildren().size());
        assertEquals(incomingChildrenCount,
                uip.getIncomingData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()).getChildren().size());
        assertEquals(incomingChildrenCount + originalChildrenCount,
                uip.getModifiedData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()).getChildren().size());
    }

    @Test
    public void replaceMODSToObjectWithoutMODS() throws Exception {
        InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUpdateMODS.xml"));
        Abdera abdera = new Abdera();
        Parser parser = abdera.getParser();
        Document<Entry> entryDoc = parser.parse(entryPart);
        Entry entry = entryDoc.getRoot();

        AccessClient accessClient = mock(AccessClient.class);
        when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString())).thenReturn(null);

        PID pid = new PID("uuid:test");

        AtomPubMetadataUIP uip = new AtomPubMetadataUIP(pid, "testuser", UpdateOperation.REPLACE, entry);
        uip.storeOriginalDatastreams(accessClient);

        assertFalse(uip.getOriginalData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
        assertFalse(uip.getModifiedData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
        assertTrue(uip.getIncomingData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));

        filter.doFilter(uip);

        assertFalse(uip.getOriginalData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
        assertTrue(uip.getModifiedData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
        assertTrue(uip.getIncomingData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
    }

    @Test
    public void replaceMODSOnObjectWithMODS() throws Exception {
        InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUpdateMODS.xml"));
        Abdera abdera = new Abdera();
        Parser parser = abdera.getParser();
        Document<Entry> entryDoc = parser.parse(entryPart);
        Entry entry = entryDoc.getRoot();

        AccessClient accessClient = mock(AccessClient.class);
        MIMETypedStream modsStream = new MIMETypedStream();
        File raf = new File("src/test/resources/testmods.xml");
        byte[] bytes = FileUtils.readFileToByteArray(raf);
        modsStream.setStream(bytes);
        modsStream.setMIMEType("text/xml");
        when(
                accessClient.getDatastreamDissemination(any(PID.class),
                        eq(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()), anyString())).thenReturn(modsStream);

        PID pid = new PID("uuid:test");

        AtomPubMetadataUIP uip = new AtomPubMetadataUIP(pid, "testuser", UpdateOperation.REPLACE, entry);
        uip.storeOriginalDatastreams(accessClient);

        assertTrue(uip.getOriginalData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
        assertFalse(uip.getModifiedData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
        assertTrue(uip.getIncomingData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));

        int incomingChildrenCount = uip.getIncomingData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName())
                .getChildren().size();

        filter.doFilter(uip);

        assertEquals(incomingChildrenCount,
                uip.getIncomingData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()).getChildren().size());
        assertEquals(incomingChildrenCount,
                uip.getModifiedData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()).getChildren().size());
        // Assert that the new modified object isn't the incoming object
        assertFalse(uip.getModifiedData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName())
                .equals(uip.getIncomingData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName())));
    }

    @Test
    public void invalidMODS() throws Exception {
        SAXBuilder builder = new SAXBuilder();

        InputStream modsStream = new FileInputStream(new File("src/test/resources/invalidMODS.xml"));
        org.jdom2.Document modsDoc = builder.build(modsStream);
        org.jdom2.Element modsElement = modsDoc.detachRootElement();

        PID pid = new PID("uuid:test");
        MetadataUIP uip = new MetadataUIP(pid, "testuser", UpdateOperation.ADD);
        uip.getIncomingData().put(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), modsElement);

        try {
            filter.doFilter(uip);
            fail();
        } catch (UIPException expected) {
            assertTrue(expected.getMessage().startsWith("MODS failed to validate to schema"));
            log.debug("Validation Message: " + expected.getMessage());
        }
    }

    @Test
    public void wrongUIPType() throws UIPException {
        ContentUIP uip = mock(ContentUIP.class);

        filter.doFilter(uip);

        verify(uip, times(0)).getIncomingData();
        verify(uip, times(0)).getModifiedData();
    }

    @Test
    public void nullUIP() throws UIPException {
        MODSValidationUIPFilter filter = new MODSValidationUIPFilter();
        filter.doFilter(null);
        // Passes if no exception
    }

    public SchematronValidator getSchematronValidator() {
        return schematronValidator;
    }

    public void setSchematronValidator(SchematronValidator schematronValidator) {
        this.schematronValidator = schematronValidator;
    }
}
