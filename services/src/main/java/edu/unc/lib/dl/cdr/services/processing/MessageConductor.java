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

import edu.unc.lib.dl.cdr.services.model.PIDMessage;

public interface MessageConductor {
	void add(PIDMessage message);
	
	/**
	 * Returns the identifier string for this conductor
	 * @return
	 */
	String getIdentifier();
	
	/**
	 * Halt processing of the conductor queue
	 */
	void pause();
	
	/**
	 * Resume processing of the conductor queue
	 */
	void resume();
	
	/**
	 * Whether the conductor queue processing is paused or not
	 * @return
	 */
	boolean isPaused();
	
	/**
	 * Get the number of messages queued for processing
	 * @return
	 */
	int getQueueSize();
	
	/**
	 * Clear the queue(s).  confirm parameter must be "yes" for the operation to occur
	 * @param confirm
	 */
	void clearQueue(String confirm);
	
	/**
	 * Clears message processing state objects.
	 * @param confirm
	 */
	void clearState(String confirm);
	
	/**
	 * True if there are no messages being processed or queued to be processed
	 * @return
	 */
	boolean isEmpty();
	
	/**
	 * True if the conductor is not actively processing messages.
	 * @return
	 */
	boolean isIdle();
	
	/**
	 * Indicates if the conductor is ready to receive new messages.
	 * @return
	 */
	boolean isReady();
	
	/**
	 * Shutdown the conductors executor, preventing no future runnables from being added
	 */
	void shutdown(String confirm);
	
	/**
	 * Shuts down the executor immediately, aborting and clearing the queue of runnables
	 * @param confirm
	 */
	void shutdownNow(String confirm);
	
	/**
	 * Attempts to interrupt the currently running workers and stop execution, but retain
	 * future runnables.
	 * @param confirm
	 */
	void abort(String confirm);
	
	/**
	 * Restart the executor after a shutdown
	 */
	void restart();
	
	String queuesToString();
	
	String getConductorStatus();
}
