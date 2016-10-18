/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.fcrepo4;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;

import java.net.URI;
import java.util.regex.Matcher;

import edu.unc.lib.dl.fedora.PID;

/**
 * Provides static methods for creating PID objects
 * 
 * @author bbpennel
 *
 */
public class PIDs {

	private static Repository repository;

	/**
	 * Get a PID object for the given URI.
	 * 
	 * @param uri
	 * @return new PID object for the given URI
	 */
	public static PID get(URI uri) {
		return get(uri.toString());
	}

	/**
	 * Get a PID object for the given identifier or URI string. Should either be
	 * a fully qualified repository URI or follow the syntax for an identifier,
	 * such as: deposit/uuid:0411cf7e-9ac0-4ab0-8c24-ff367e8e77f6
	 * 
	 * @param value
	 * @return new PID object for the given identifier or URI
	 */
	public static PID get(String value) {
		if (value == null) {
			return null;
		}

		String id;
		String qualifier;
		String componentPath;
		String repositoryPath;

		if (value.startsWith(repository.getFedoraBase())) {
			// Given value was a fedora path. Remove the base and decompose
			repositoryPath = value;
			String path = value.substring(repository.getFedoraBase().length());

			Matcher matcher = RepositoryPathConstants.repositoryPathPattern.matcher(path);
			if (matcher.matches()) {
				// extract the qualifier/category portion of the path, ex: deposit, content, etc.
				qualifier = matcher.group(1);
				// store the trailing component path, which is everything after the object identifier
				componentPath = matcher.group(5);
				// store the identifier for the main object
				id = matcher.group(3);
			} else {
				// Value was an invalid path within the repository
				return null;
			}
		} else {
			// Determine if the value matches the pattern for an identifier
			Matcher matcher = RepositoryPathConstants.identifierPattern.matcher(value);
			if (matcher.matches()) {
				// Store the qualifier if specified, otherwise use the default "content" qualifier
				qualifier = matcher.group(2);
				if (qualifier == null) {
					qualifier = RepositoryPathConstants.CONTENT_BASE;
				}
				// store the identifier for the main object
				id = matcher.group(4);
				// store the trailing component path
				componentPath = matcher.group(6);
				// Expand the identifier into a repository path
				repositoryPath = getRepositoryPath(id, qualifier, componentPath);
			} else {
				// No a recognized format for constructing a pid
				return null;
			}
		}

		// Build and return the new pid object
		return new FedoraPID(id, qualifier, componentPath, URI.create(repositoryPath));
	}

	/**
	 * Get a PID object with the given qualifier and id
	 * 
	 * @param qualifier
	 * @param id
	 * @return
	 */
	public static PID get(String qualifier, String id) {
		return get(qualifier + "/" + id);
	}

	public static void setRepository(Repository repository) {
		PIDs.repository = repository;
	}

	/**
	 * Expands the identifier for a repository object into the full repository path.
	 * 
	 * @param id
	 * @param qualifier
	 * @param componentPath
	 * @return
	 */
	private static String getRepositoryPath(String id, String qualifier, String componentPath) {
		StringBuilder builder = new StringBuilder(repository.getFedoraBase());
		builder.append(qualifier).append('/');
		// Chunk the id into 
		for (int i = 0; i < HASHED_PATH_DEPTH; i++) {
			builder.append(id.substring(i * HASHED_PATH_SIZE, i * HASHED_PATH_SIZE + HASHED_PATH_SIZE))
					.append('/');
		}
		builder.append(id);
		if (componentPath != null) {
			builder.append('/').append(componentPath);
		}
		return builder.toString();
	}
}
