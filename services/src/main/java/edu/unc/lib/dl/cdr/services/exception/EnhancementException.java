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
package edu.unc.lib.dl.cdr.services.exception;

import edu.unc.lib.dl.fedora.PID;

public class EnhancementException extends Exception {
	private static final long serialVersionUID = 1L;

	private Severity severity = Severity.UNRECOVERABLE;
	
	public EnhancementException(Throwable cause){
		this.setStackTrace(cause.getStackTrace());
	}
	
	public EnhancementException(Throwable cause, Severity severity){
		this.setStackTrace(cause.getStackTrace());
		this.severity = severity;
	}

	public EnhancementException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public EnhancementException(String message, Throwable cause, Severity severity) {
		super(severity + ": " + message, cause);
		this.severity = severity;
	}

	public EnhancementException(PID pid, String message) {
		super("Enhancement failed for "+pid.getPid()+":\t"+message);
	}
	
	public EnhancementException(PID pid, String message, Throwable cause, Severity severity) {
		super(severity + ": " + "Enhancement failed for "+pid.getPid()+":\t"+message, cause);
		this.severity = severity;
	}

	public Severity getSeverity() {
		return severity;
	}

	public void setSeverity(Severity severity) {
		this.severity = severity;
	}

	public static enum Severity {
		RECOVERABLE("recoverable"),
		UNRECOVERABLE("unrecoverable"),
		FATAL("fatal");
		
		private String name;
		
		private Severity(String name){
			this.name = name;
		}
		
		public String toString(){
			return name;
		}
	}
}