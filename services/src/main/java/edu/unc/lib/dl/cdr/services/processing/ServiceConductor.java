/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.processing;

import java.util.Map;

/**
 * @author Gregory Jansen
 *
 */
public interface ServiceConductor {

	/**
	 * Returns the identifier string for this conductor
	 * @return
	 */
	public String getIdentifier();

	/**
	 * Halt processing of the conductor queue
	 */
	public void pause();

	/**
	 * Resume processing of the conductor queue
	 */
	public void resume();

	/**
	 * Whether the conductor queue processing is paused or not
	 * @return
	 */
	public boolean isPaused();

	/**
	 * True if there are no messages being processed or queued to be processed
	 * @return
	 */
	public boolean isEmpty();

	/**
	 * True if the conductor is not actively processing messages.
	 * @return
	 */
	public boolean isIdle();

	/**
	 * Shutdown the conductors executor, preventing no future runnables from being added
	 */
	public void shutdown();

	/**
	 * Shuts down the executor immediately, aborting and clearing the queue of runnables\
	 */
	public void shutdownNow();

	/**
	 * Attempts to interrupt the currently running workers and stop execution, but retain
	 * future runnables.
	 */
	public void abort();

	/**
	 * Restart the executor after a shutdown
	 */
	public void restart();

	public int getActiveThreadCount();

}