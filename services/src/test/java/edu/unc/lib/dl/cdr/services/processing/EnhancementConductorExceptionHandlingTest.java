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
package edu.unc.lib.dl.cdr.services.processing;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.imaging.ImageEnhancement;
import edu.unc.lib.dl.cdr.services.imaging.ImageEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancement;
import edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancementService;
import edu.unc.lib.dl.cdr.services.model.FailedObjectHashMap;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancement;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class EnhancementConductorExceptionHandlingTest extends Assert{

	private TechnicalMetadataEnhancementService techmd;
	private ThumbnailEnhancementService thumb;
	private ImageEnhancementService image;
	private TechnicalMetadataEnhancement techmdEnhancement;
	private ThumbnailEnhancement thumbEnhancement;
	private ImageEnhancement imageEnhancement;
	private EnhancementConductor enhancementConductor;
	private List<ObjectEnhancementService> services;
	
	int numberTestMessages;
	
	@Before
	public void setUp() throws Exception {
		techmd = mock(TechnicalMetadataEnhancementService.class);
		thumb = mock(ThumbnailEnhancementService.class);
		image = mock(ImageEnhancementService.class);
		
		when(techmd.isActive()).thenReturn(true);
		when(thumb.isActive()).thenReturn(true);
		when(image.isActive()).thenReturn(true);
		
		when(techmd.prefilterMessage(any(PIDMessage.class))).thenReturn(true);
		when(thumb.prefilterMessage(any(PIDMessage.class))).thenReturn(true);
		when(image.prefilterMessage(any(PIDMessage.class))).thenReturn(true);
		
		when(techmd.isApplicable(any(PIDMessage.class))).thenReturn(true);
		when(thumb.isApplicable(any(PIDMessage.class))).thenReturn(true);
		when(image.isApplicable(any(PIDMessage.class))).thenReturn(true);
		
		techmdEnhancement = mock(TechnicalMetadataEnhancement.class);
		thumbEnhancement = mock(ThumbnailEnhancement.class);
		imageEnhancement = mock(ImageEnhancement.class);
		when(techmd.getEnhancement(any(PIDMessage.class))).thenReturn(techmdEnhancement);
		when(thumb.getEnhancement(any(PIDMessage.class))).thenReturn(thumbEnhancement);
		when(image.getEnhancement(any(PIDMessage.class))).thenReturn(imageEnhancement);
		
		services = new ArrayList<ObjectEnhancementService>();
		services.add(techmd);
		services.add(thumb);
		services.add(image);
		
		enhancementConductor = new EnhancementConductor();
		enhancementConductor.setServices(services);
		enhancementConductor.setRecoverableDelay(0);
		enhancementConductor.setUnexpectedExceptionDelay(0);
		enhancementConductor.setMaxThreads(3);
		enhancementConductor.setFailedPids(new FailedObjectHashMap());
		enhancementConductor.init();
		
		numberTestMessages = 10;
	}
	
	@Test
	public void recoverableEnhancementFailure() throws EnhancementException{
		EnhancementException exception = mock(EnhancementException.class);
		when(exception.getSeverity()).thenReturn(EnhancementException.Severity.RECOVERABLE);
		
		doThrow(exception).when(techmdEnhancement).call();
		doThrow(exception).when(imageEnhancement).call();
		
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:"+i, JMSMessageUtil.fedoraMessageNamespace, 
					JMSMessageUtil.FedoraActions.INGEST.getName());
			message.setFilteredServices(services);
			enhancementConductor.add(message);
		}
		
		while (!enhancementConductor.isEmpty());
		
		verify(exception, times(numberTestMessages * 2)).printStackTrace(any(PrintWriter.class));
		
		verify(techmdEnhancement, times(numberTestMessages * 2)).call();
		verify(thumbEnhancement, times(numberTestMessages)).call();
		verify(imageEnhancement, times(numberTestMessages * 2)).call();
	}
	
	//@Test
	public void stressFatalEnhancementFailure() throws Exception{
		for (int i=0; i<50; i++){
			fatalEnhancementFailure();
			setUp();
		}
	}
	
	@Test
	public void fatalEnhancementFailure() throws Exception{
		EnhancementException exception = mock(EnhancementException.class);
		when(exception.getSeverity()).thenReturn(EnhancementException.Severity.FATAL);
		
		ArrayList<ObjectEnhancementService> unexceptionalServices = new ArrayList<ObjectEnhancementService>();
		unexceptionalServices.add(thumb);
		
		doThrow(exception).when(techmdEnhancement).call();
		doThrow(exception).when(imageEnhancement).call();
		
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:"+i, JMSMessageUtil.fedoraMessageNamespace, 
					JMSMessageUtil.FedoraActions.INGEST.getName());
			message.setFilteredServices(unexceptionalServices);
			enhancementConductor.add(message);
		}
		
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:"+(i+numberTestMessages), JMSMessageUtil.fedoraMessageNamespace, 
					JMSMessageUtil.FedoraActions.INGEST.getName());
			message.setFilteredServices(services);
			enhancementConductor.add(message);
		}
		
		while (enhancementConductor.getLockedPids().size() == enhancementConductor.getMaxThreads()
				|| (!enhancementConductor.isPaused() && !enhancementConductor.isEmpty()));
		
		assertTrue(enhancementConductor.isPaused());
		//Give it a moment for all runnables to end or reach pause state.
		Thread.sleep(100L);
		
		int numberAborted = enhancementConductor.getFailedPids().size();
		//System.out.println(enhancementConductor.queuesToString());
		
		//Each aborted runnable should have printed a stack trace
		verify(exception, times(numberAborted)).printStackTrace(any(PrintWriter.class));
		//Tech also throws an error, every invocation of which should end processing
		verify(techmdEnhancement, times(numberAborted)).call();
		//Image should never be reached.
		verify(imageEnhancement, never()).call();
		//Number of thumbs runs depends on order in which threads finish, so only predictable within one window
		verify(thumbEnhancement, atLeast(numberTestMessages - enhancementConductor.getLockedPids().size())).call();
		verify(thumbEnhancement, atMost(numberTestMessages)).call();
		
		assertEquals(enhancementConductor.getFailedPids().size(), numberAborted);
	}
	
	@Test
	public void unrecoverableEnhancementFailure() throws EnhancementException{
		EnhancementException exception = mock(EnhancementException.class);
		when(exception.getSeverity()).thenReturn(EnhancementException.Severity.UNRECOVERABLE);
		
		ArrayList<ObjectEnhancementService> unexceptionalServices = new ArrayList<ObjectEnhancementService>();
		unexceptionalServices.add(thumb);
		
		doThrow(exception).when(techmdEnhancement).call();
		doThrow(exception).when(imageEnhancement).call();
		
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:"+i, JMSMessageUtil.fedoraMessageNamespace, 
					JMSMessageUtil.FedoraActions.INGEST.getName());
			message.setFilteredServices(unexceptionalServices);
			enhancementConductor.add(message);
		}
		
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:"+(i+numberTestMessages), JMSMessageUtil.fedoraMessageNamespace, 
					JMSMessageUtil.FedoraActions.INGEST.getName());
			message.setFilteredServices(services);
			enhancementConductor.add(message);
		}
		
		while (!enhancementConductor.isEmpty());
		
		//Two failing services
		verify(exception, times(numberTestMessages * 2)).printStackTrace(any(PrintWriter.class));
		
		verify(techmdEnhancement, times(numberTestMessages)).call();
		verify(imageEnhancement, times(numberTestMessages)).call();
		verify(thumbEnhancement, times(numberTestMessages * 2)).call();
		
		assertEquals(enhancementConductor.getFailedPids().size(), numberTestMessages);
		
		assertFalse(enhancementConductor.isPaused());
	}
	
	@Test
	public void runtimeEnhancementFailure() throws EnhancementException{
		RuntimeException exception = mock(RuntimeException.class);
		
		doThrow(exception).when(techmdEnhancement).call();
		doThrow(exception).when(imageEnhancement).call();
		
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:"+i, JMSMessageUtil.fedoraMessageNamespace, 
					JMSMessageUtil.FedoraActions.INGEST.getName());
			message.setFilteredServices(services);
			enhancementConductor.add(message);
		}
		
		while (!enhancementConductor.isEmpty());
		
		verify(exception, times(numberTestMessages * 2)).printStackTrace(any(PrintWriter.class));
		
		verify(techmdEnhancement, times(numberTestMessages * 2)).call();
		verify(thumbEnhancement, times(numberTestMessages)).call();
		verify(imageEnhancement, times(numberTestMessages * 2)).call();
	}
}
