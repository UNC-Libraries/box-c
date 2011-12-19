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
package edu.unc.lib.dl.cdr.services.sword.server;

import javax.servlet.ServletContext;

import org.swordapp.server.SwordConfiguration;

/**
 * 
 * @author bbpennel
 *
 */
public class SwordConfigurationImpl implements SwordConfiguration {
	private String authType = null;
	private int maxUploadSize = 0;
	private String tempDirectory = null;

	public SwordConfigurationImpl() {
	}

	public void init(ServletContext context) {
		this.authType = context.getInitParameter("authentication-method");
		// this.tempDirectory = context.getInitParameter("");
		// this.authType = context.getInitParameter("authentication-method");
	}

	public boolean returnDepositReceipt() {
		return true;
	}

	public boolean returnStackTraceInError() {
		return true;
	}

	public boolean returnErrorBody() {
		return true;
	}

	public String generator() {
		return "http://www.swordapp.org/";
	}

	public String generatorVersion() {
		return "2.0";
	}

	public String administratorEmail() {
		return null;
	}

	public String getAuthType() {
		return this.authType;
	}

	public boolean storeAndCheckBinary() {
		return false;
	}

	public String getTempDirectory() {
		return this.tempDirectory;
	}

	public int getMaxUploadSize() {
		return this.maxUploadSize;
	}

}
