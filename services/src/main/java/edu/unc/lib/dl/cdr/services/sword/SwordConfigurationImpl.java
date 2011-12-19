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
package edu.unc.lib.dl.cdr.services.sword;


import javax.annotation.Resource;

import org.swordapp.server.SwordConfiguration;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * 
 * @author bbpennel
 *
 */
public class SwordConfigurationImpl implements SwordConfiguration {
	private String authType = null;
	private int maxUploadSize = 0;
	private String tempDirectory = null;
	@Resource
	private TripleStoreQueryService tripleStoreQueryService;
	private PID collectionsPidObject;

	public SwordConfigurationImpl() {
	}

	public void init() {
		//this.authType = context.getInitParameter("authentication-method");
		collectionsPidObject = tripleStoreQueryService.fetchByRepositoryPath("/Collections");
	}

	@Override
	public boolean returnDepositReceipt() {
		return true;
	}

	@Override
	public boolean returnStackTraceInError() {
		return true;
	}

	@Override
	public boolean returnErrorBody() {
		return true;
	}

	@Override
	public String generator() {
		return "http://www.swordapp.org/";
	}

	@Override
	public String generatorVersion() {
		return "2.0";
	}

	@Override
	public String administratorEmail() {
		return null;
	}

	@Override
	public String getAuthType() {
		return this.authType;
	}

	public void setAuthType(String authType) {
		this.authType = authType;
	}

	@Override
	public boolean storeAndCheckBinary() {
		return false;
	}

	@Override
	public String getTempDirectory() {
		return this.tempDirectory;
	}

	@Override
	public int getMaxUploadSize() {
		return this.maxUploadSize;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public PID getCollectionsPidObject() {
		return collectionsPidObject;
	}

	public void setCollectionsPidObject(PID collectionsPidObject) {
		this.collectionsPidObject = collectionsPidObject;
	}

	
}
