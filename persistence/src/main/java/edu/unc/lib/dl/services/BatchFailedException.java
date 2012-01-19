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
package edu.unc.lib.dl.services;

/**
 * @author Gregory Jansen
 *
 */
public class BatchFailedException extends RuntimeException {

	/**
	 * @param string
	 */
	public BatchFailedException(String string) {
		super(string);
	}

	/**
	 * @param string
	 * @param e
	 */
	public BatchFailedException(String string, Throwable e) {
		super(string, e);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 426840637745292474L;

}
