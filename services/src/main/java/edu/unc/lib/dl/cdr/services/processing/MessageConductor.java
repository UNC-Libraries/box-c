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
	 * Get the number of messages queued for processing
	 * @return
	 */
	int getQueueSize();

	/**
	 * Returns the identifier string for this conductor
	 * @return
	 */
	public String getIdentifier();

	/**
	 * Clear the queue(s).  confirm parameter must be "yes" for the operation to occur
	 */
	void clearQueue();

	/**
	 * Clears message processing state objects.
	 */
	void clearState();

	String queuesToString();

	/**
	 * Indicates if the conductor is ready to receive new messages.
	 * @return
	 */
	public boolean isReady();
}
