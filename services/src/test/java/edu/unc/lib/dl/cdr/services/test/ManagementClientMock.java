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
package edu.unc.lib.dl.cdr.services.test;

import org.jdom.Document;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.FileSystemException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.ObjectExistsException;
import edu.unc.lib.dl.fedora.PID;

public class ManagementClientMock extends ManagementClient {

	public static final String PID_FILE_SYSTEM = "uuid:filesystem";
	public static final String PID_NOT_FOUND = "uuid:notfound";
	public static final String PID_DUPLICATE = "uuid:duplicate";
	public static final String PID_FEDORA = "uuid:fedora";
	public static final String PID_EXCEPTION = "uuid:exception";
	public static final String PID_RUN_TIME = "uuid:runtime";
	
	private void throwException(PID pid) throws FedoraException {
		if (PID_FILE_SYSTEM.equals(pid.getPid()))
			throw new FileSystemException("");
		if (PID_NOT_FOUND.equals(pid.getPid()))
			throw new NotFoundException("");
		if (PID_DUPLICATE.equals(pid.getPid()))
			throw new ObjectExistsException("");
		if (PID_FEDORA.equals(pid.getPid()))
			throw new FedoraException("");
		if (PID_RUN_TIME.equals(pid.getPid()))
			throw new RuntimeException("");
	}
	
	@Override
	public Document getObjectXML(PID pid) throws FedoraException {
		throwException(pid);
		return null;
	}
}
