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

import java.util.regex.Pattern;

/**
 * Constants for constructing standard paths within the repository.
 * 
 * @author bbpennel
 *
 */
public class RepositoryPathConstants {

	public static final String CONTENT_BASE = "content";
	
	public static final String DEPOSIT_RECORD_BASE = "deposits";
	
	public static final String DEPOSIT_MANIFEST_CONTAINER = "manifest";
	
	public static final String EVENTS_CONTAINER = "event";
	
	public static final int HASHED_PATH_DEPTH = 4;
	
	public static final int HASHED_PATH_SIZE = 2;
	
	// Regex pattern for decomposing a repository URI for an object or component of an object
	public static final Pattern repositoryPathPattern = Pattern
			.compile("/?([a-zA-Z]+)/([a-f0-9]{" + HASHED_PATH_SIZE + "}/){" + HASHED_PATH_DEPTH + "}"
					+ "([a-f0-9\\\\-]+)(/(.+)?)?");
	
	// Regex pattern for decomposing an identifier for an object or component
	public static final Pattern identifierPattern = Pattern
			.compile("(([a-zA-Z]+)/)?(uuid:)?([a-f0-9\\-]+)(/(.+)?)?");
}
