/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.event;

import java.io.File;

import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fedora.PID;

/**
 * A factory class for creating PremisLogger instances
 * 
 * @author harring
 */

public class PremisLoggerFactory {
	
	public PremisLogger createPremisLogger(PID pid, File file, Repository repository) {
		return new FilePremisLogger(pid, file, repository);
	}
	
	public PremisLogger createPremisLogger(RepositoryObject repoObject, Repository repository) {
		return new RepositoryPremisLogger(repoObject, repository);
	}

}
