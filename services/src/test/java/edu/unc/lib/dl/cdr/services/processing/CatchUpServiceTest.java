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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.imaging.ImageEnhancementService;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FailedObjectHashMap;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.fedora.PID;

import static org.mockito.Mockito.*;

public class CatchUpServiceTest extends Assert {

	private CatchUpService catchup;
	MessageDirector messageDirector;
	EnhancementConductor enhancementConductor;
	TechnicalMetadataEnhancementService techmd;
	ImageEnhancementService image;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		catchup = new CatchUpService();
		messageDirector = mock(MessageDirector.class);
		enhancementConductor = mock(EnhancementConductor.class);
		when(enhancementConductor.isEmpty()).thenReturn(true);
		List<EnhancementMessage> collisionList = mock(List.class);
		when(collisionList.size()).thenReturn(0);
		when(enhancementConductor.getCollisionList()).thenReturn(collisionList);

		BlockingQueue<EnhancementMessage> pidQueue = mock(BlockingQueue.class);
		when(pidQueue.size()).thenReturn(0);
		when(enhancementConductor.getPidQueue()).thenReturn(pidQueue);

		FailedObjectHashMap failedPids = mock(FailedObjectHashMap.class);
		when(failedPids.size()).thenReturn(0);
		when(enhancementConductor.getFailedPids()).thenReturn(failedPids);

		techmd = mock(TechnicalMetadataEnhancementService.class);
		image = mock(ImageEnhancementService.class);

		List<ObjectEnhancementService> services = new ArrayList<ObjectEnhancementService>();
		services.add(techmd);
		services.add(image);

		catchup.setMessageDirector(messageDirector);
		catchup.setenhancementConductor(enhancementConductor);
		catchup.setServices(services);
		catchup.setCatchUpCheckDelay(100L);
		catchup.setEnabled(true);
	}

	@Test
	public void activation() {
		catchup.setEnabled(false);
		catchup.activate();
		assertFalse(catchup.isEnabled());
		assertFalse(catchup.isActive());

		catchup.setEnabled(true);
		catchup.activate();
		assertTrue(catchup.isEnabled());
		assertTrue(catchup.isActive());
	}

	@Test
	public void activationInvalidDate() {
		String priorToDate = "invalid";
		try {
			catchup.activate(priorToDate);
			fail();
		} catch (IllegalArgumentException e) {
			assertFalse(catchup.isActive());
		}

		priorToDate = "";
		try {
			catchup.activate(priorToDate);
			fail();
		} catch (IllegalArgumentException e) {
			assertFalse(catchup.isActive());
		}

		priorToDate = "2011-4-4T5:5:5.555";
		try {
			catchup.activate(priorToDate);
			fail();
		} catch (IllegalArgumentException e) {
			assertFalse(catchup.isActive());
		}

		priorToDate = "2011-04-04T05:05";
		try {
			catchup.activate(priorToDate);
			fail();
		} catch (IllegalArgumentException e) {
			assertFalse(catchup.isActive());
		}

		priorToDate = "2011-04-04T05:05:05.555";
		try {
			catchup.activate(priorToDate);
			fail();
		} catch (IllegalArgumentException e) {
			assertFalse(catchup.isActive());
		}
	}

	@Test
	public void activationValidDate() {
		String priorToDate = "2011-04-04T05:05:05.555Z";
		catchup.activate(priorToDate);
		assertTrue(catchup.isActive());

		priorToDate = "2011-04-04T05:05:05Z";
		catchup.activate(priorToDate);
		assertTrue(catchup.isActive());
	}

	@Test
	public void successfulAddBatch() throws EnhancementException {
		when(techmd.isActive()).thenReturn(true);
		when(image.isActive()).thenReturn(true);
		// Techmd should return results on the first call.
		when(techmd.findCandidateObjects(anyInt())).thenAnswer(new Answer<List<PID>>() {
			private boolean firstCall = true;

			public List<PID> answer(InvocationOnMock invocation) throws Throwable {
				if (firstCall) {
					firstCall = false;
					int resultCount = (Integer) invocation.getArguments()[0];
					return getResultSet(resultCount);
				}
				return new ArrayList<PID>();
			}
		});

		catchup.setPageSize(50);

		catchup.activate();
		verify(messageDirector, times(catchup.getPageSize())).direct(any(PIDMessage.class));
		verify(techmd, times(2)).findCandidateObjects(anyInt());
		verify(image, times(2)).findCandidateObjects(anyInt());
		assertTrue(catchup.isActive());
	}

	@Test
	public void successfulAddStale() throws EnhancementException {
		when(techmd.isActive()).thenReturn(true);
		when(image.isActive()).thenReturn(true);
		// Techmd should return results on the first call.
		when(techmd.findStaleCandidateObjects(anyInt(), anyString())).thenAnswer(new Answer<List<PID>>() {
			private boolean firstCall = true;

			public List<PID> answer(InvocationOnMock invocation) throws Throwable {
				if (firstCall) {
					firstCall = false;
					int resultCount = (Integer) invocation.getArguments()[0];
					return getResultSet(resultCount);
				}
				return new ArrayList<PID>();
			}
		});

		catchup.setPageSize(50);

		catchup.activate("2011-04-04T05:05:05.555Z");
		verify(messageDirector, times(catchup.getPageSize())).direct(any(PIDMessage.class));
		verify(techmd, times(2)).findStaleCandidateObjects(anyInt(), anyString());
		verify(image, times(2)).findStaleCandidateObjects(anyInt(), anyString());
		assertTrue(catchup.isActive());
	}

	@Test
	public void deactivateDuringRun() throws EnhancementException {
		when(techmd.isActive()).thenReturn(true);
		when(image.isActive()).thenReturn(true);
		
		List<PID> results = getResultSet(catchup.getPageSize()); 
		when(techmd.findCandidateObjects(anyInt())).thenReturn(results);
		
		//Deactivate after a short delay
		int delay = 500;
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				catchup.deactivate();
			}
		}, delay);

		catchup.activate();
		
		while (catchup.isActive());
		
		assertFalse(catchup.isActive());
		verify(messageDirector, atLeast(catchup.getPageSize())).direct(any(PIDMessage.class));
		verify(techmd, atLeastOnce()).findCandidateObjects(anyInt());
		verify(image, atLeastOnce()).findCandidateObjects(anyInt());
	}
	
	/**
	 * Verify that extra calls to the catchup method cannot run while it is already executing.
	 * @throws EnhancementException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test
	public void lockoutCatchUp() throws EnhancementException, InterruptedException, ExecutionException{
		when(techmd.isActive()).thenReturn(true);
		when(image.isActive()).thenReturn(true);
		
		List<PID> results = getResultSet(catchup.getPageSize()); 
		when(techmd.findCandidateObjects(anyInt())).thenReturn(results);
		
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		Runnable activateRunnable = new Runnable() {
			public void run(){
				while (!catchup.isActive());
				boolean response = catchup.activate();
				assertFalse(response);
			}
		};
		Runnable deactivateRunnable = new Runnable() {
			public void run(){
				catchup.deactivate();
				System.out.println("Deactivated " + System.currentTimeMillis());
			}
		};
		
		// Execute a second activation after the original has been running for 100ms
		ScheduledFuture<?> activateHandler = scheduler.schedule(activateRunnable, 100, TimeUnit.MILLISECONDS);
		ScheduledFuture<?> deactivateHandler = scheduler.schedule(deactivateRunnable, 500, TimeUnit.MILLISECONDS);
		
		//Begin the first catchup call.
		boolean response = catchup.activate();
		assertTrue(response);
		
		// Spit out any exceptions from the activateRunnable, in case of assertion failure.
		deactivateHandler.get();
		activateHandler.get();
		scheduler.shutdown();
	}
	
	private List<PID> getResultSet(int resultCount){
		List<PID> results = new ArrayList<PID>();
		for (int i = 0; i < resultCount; i++) {
			results.add(new PID("uuid:" + i));
		}
		return results;
	}
}
