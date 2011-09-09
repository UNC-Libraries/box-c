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
package fedorax.server.module.storage.lowlevel.irods;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.io.FileIOOperations;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileInputStream;

/**
 * @author Mike Conway - DICE (www.irods.org)
 *
 */
public class SessionClosingInputStream extends IRODSFileInputStream {

	protected SessionClosingInputStream(IRODSFile irodsFile2,
			FileIOOperations fileIOOperations) throws FileNotFoundException {
		super(irodsFile2, fileIOOperations);
	}

	@Override
	public void close() throws IOException {
		super.close();
		try {
			this.getFileIOOperations().getIRODSSession().closeSession();
		} catch (JargonException e) {
			throw new IOException("error in close session returned as IOException for method contracts");
		}
	}

}
