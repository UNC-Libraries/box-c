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
/**
 *
 */
package edu.unc.lib.dl.ingest.sip;

import org.jdom.Document;

import edu.unc.lib.dl.ingest.IngestException;

/**
 * @author Gregory Jansen
 * 
 */
public class InvalidMETSException extends IngestException {

	/**
     *
     */
	private static final long serialVersionUID = 7012398949440082066L;
	private Document svrl;

	public Document getSvrl() {
		return svrl;
	}

	/**
	 * @param msg
	 */
	public InvalidMETSException(String msg) {
		super(msg);
	}

	public InvalidMETSException(String msg, Throwable e) {
		super(msg, e);
	}

	/**
	 * @param msg
	 *           error message
	 * @param svrl
	 *           Schematron validation report Document
	 */
	public InvalidMETSException(String msg, Document svrl) {
		super(msg);
		this.svrl = svrl;
	}

}
