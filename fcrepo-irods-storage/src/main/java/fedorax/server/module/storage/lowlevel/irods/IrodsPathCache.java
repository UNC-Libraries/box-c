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
package fedorax.server.module.storage.lowlevel.irods;

import java.util.WeakHashMap;

/**
 * This class implements a cache of the iRODS paths to FOXML and data stream files. The implementation is a simple map
 * with weak keys.
 *
 * @author Gregory Jansen
 *
 */
public class IrodsPathCache {
	protected WeakHashMap<String, String> map = new WeakHashMap<String, String>();

	/* This put() will return any previous value already at index i. */
	public void put(String fedoraID, String irodsPath) {
		map.put(fedoraID, irodsPath);
	}

	/*
	 * This get() will return the object at index i, or null if no object was put there, or if the object was garbage
	 * collected.
	 */
	public String get(String fedoraID) {
		return map.get(fedoraID);
	}

}
