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

package edu.unc.lib.dl.cdr.services.techmd;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.cdr.services.AbstractIrodsObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class TechnicalMetadataEnhancementTest extends Assert {

	@Mock
	private EnhancementMessage message;
	@Mock
	private AbstractIrodsObjectEnhancementService service;
	@Mock
	private ManagementClient managementClient;
	@Mock
	private TripleStoreQueryService tsqs;
	
	private TechnicalMetadataEnhancement enhance;

	@Before
	public void setup() throws Exception {
		initMocks(this);
		
		when(service.getManagementClient()).thenReturn(managementClient);
		when(managementClient.getIrodsPath(anyString())).thenReturn("/");
		when(message.getPid()).thenReturn(new PID("uuid:item"));
		when(service.isActive()).thenReturn(true);
		when(service.getTripleStoreQueryService()).thenReturn(tsqs);
		
		enhance = new TechnicalMetadataEnhancement(service, message);
		
	}
	
	@Test
	public void leadingLoggingStatementsTest() throws Exception {
		
		InputStream inStream = this.getClass().getResourceAsStream("fitsOutputMultipleLineBreaks.xml");
		when(service.remoteExecuteWithPhysicalLocation(anyString(), anyString())).thenReturn(inStream);
		
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(this.getClass().getResourceAsStream("imageFOXML.xml"));
		when(message.getFoxml()).thenReturn(foxml);
		
		enhance.call();
		
		verify(managementClient).addManagedDatastream(any(PID.class), eq(Datastream.MD_TECHNICAL.getName()),
				anyBoolean(), anyString(), anyListOf(String.class), anyString(), anyBoolean(), anyString(), anyString());
		
		verify(managementClient).setExclusiveLiteral(any(PID.class), eq(CDRProperty.hasSourceMimeType.getPredicate()),
				eq(CDRProperty.hasSourceMimeType.getNamespace()), eq("image/jpeg"), anyString());
	}
	
	@Test
	public void mimetypeEncodingTest() throws Exception {
		
		InputStream inStream = this.getClass().getResourceAsStream("fitsMimetypeEncoding.xml");
		when(service.remoteExecuteWithPhysicalLocation(anyString(), anyString())).thenReturn(inStream);
		
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(this.getClass().getResourceAsStream("unknownTypeFOXML.xml"));
		when(message.getFoxml()).thenReturn(foxml);
		
		enhance.call();
		
		verify(managementClient).addManagedDatastream(any(PID.class), eq(Datastream.MD_TECHNICAL.getName()),
				anyBoolean(), anyString(), anyListOf(String.class), anyString(), anyBoolean(), anyString(), anyString());
		
		// Check that the mimetype has had the encoding trimmed off
		verify(managementClient).setExclusiveLiteral(any(PID.class), eq(CDRProperty.hasSourceMimeType.getPredicate()),
				eq(CDRProperty.hasSourceMimeType.getNamespace()), eq("text/plain"), anyString());
	}
	
	@Test
	public void singleMimetypeResultTest() throws Exception {
		
		InputStream inStream = this.getClass().getResourceAsStream("fitsSingleResult.xml");
		when(service.remoteExecuteWithPhysicalLocation(anyString(), anyString())).thenReturn(inStream);
		
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(this.getClass().getResourceAsStream("unknownTypeFOXML.xml"));
		when(message.getFoxml()).thenReturn(foxml);
		
		enhance.call();
		
		// Check that a single 
		verify(managementClient).setExclusiveLiteral(any(PID.class), eq(CDRProperty.hasSourceMimeType.getPredicate()),
				eq(CDRProperty.hasSourceMimeType.getNamespace()), eq("audio/mp3"), anyString());
	}
	
	@Test
	public void exifMimetypeResultTest() throws Exception {
		
		InputStream inStream = this.getClass().getResourceAsStream("fitsExifResult.xml");
		when(service.remoteExecuteWithPhysicalLocation(anyString(), anyString())).thenReturn(inStream);
		
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(this.getClass().getResourceAsStream("unknownTypeFOXML.xml"));
		when(message.getFoxml()).thenReturn(foxml);
		
		enhance.call();
		
		// Check that it uses the exif mimetype
		verify(managementClient).setExclusiveLiteral(any(PID.class), eq(CDRProperty.hasSourceMimeType.getPredicate()),
				eq(CDRProperty.hasSourceMimeType.getNamespace()), eq("video/mp4"), anyString());
	}
	
	@Test
	public void symlinkConflictTest() throws Exception {
		
		InputStream inStream = this.getClass().getResourceAsStream("fitsSymlinkConflict.xml");
		when(service.remoteExecuteWithPhysicalLocation(anyString(), anyString())).thenReturn(inStream);
		
		SAXBuilder builder = new SAXBuilder();
		Document foxml = builder.build(this.getClass().getResourceAsStream("unknownTypeFOXML.xml"));
		when(message.getFoxml()).thenReturn(foxml);
		
		enhance.call();
		
		// Check that it uses the exif mimetype
		verify(managementClient).setExclusiveLiteral(any(PID.class), eq(CDRProperty.hasSourceMimeType.getPredicate()),
				eq(CDRProperty.hasSourceMimeType.getNamespace()), eq("audio/mp3"), anyString());
	}
}
