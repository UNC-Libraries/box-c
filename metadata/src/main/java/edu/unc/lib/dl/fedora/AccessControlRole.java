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
package edu.unc.lib.dl.fedora;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;

public enum AccessControlRole {
	patron("patron", Arrays.asList(CDRProperty.permitOriginalsRead, CDRProperty.permitDerivativesRead, CDRProperty.permitMetadataRead)),
	noOriginalsPatron("noOriginalsPatron", Arrays.asList(CDRProperty.permitDerivativesRead, CDRProperty.permitMetadataRead)),
	metadataOnlyPatron("metadataOnlyPatron", Arrays.asList(CDRProperty.permitMetadataRead)),
	curator("curator", Arrays.asList(CDRProperty.permitOriginalsRead, CDRProperty.permitDerivativesRead, CDRProperty.permitMetadataRead,
			CDRProperty.permitOriginalsCreate, CDRProperty.permitDerivativesCreate, CDRProperty.permitMetadataCreate,
			CDRProperty.permitOriginalsUpdate, CDRProperty.permitDerivativesUpdate, CDRProperty.permitMetadataUpdate)),
	admin("admin", Arrays.asList(CDRProperty.permitOriginalsRead, CDRProperty.permitDerivativesRead, CDRProperty.permitMetadataRead,
			CDRProperty.permitOriginalsCreate, CDRProperty.permitDerivativesCreate, CDRProperty.permitMetadataCreate,
			CDRProperty.permitOriginalsUpdate, CDRProperty.permitDerivativesUpdate, CDRProperty.permitMetadataUpdate,
			CDRProperty.permitOriginalsDelete, CDRProperty.permitDerivativesDelete, CDRProperty.permitMetadataDelete));

	private URI uri;
	private String roleName;
	private List<CDRProperty> permissions;
	private static Map<String, List<String>> permissionsMap = null;
	
	AccessControlRole(String roleName, List<CDRProperty> permissions){
		try {
			this.roleName = roleName;
			this.uri = new URI("http://cdr.unc.edu/definitions/roles#" + roleName);
			this.permissions = permissions;
		} catch (URISyntaxException e) {
			Error x = new ExceptionInInitializerError("Cannot initialize AccessControlRole");
			x.initCause(e);
			throw x;
		}
	}

	public URI getUri() {
		return uri;
	}

	public List<CDRProperty> getPermissions() {
		return permissions;
	}

	public String getRoleName() {
		return roleName;
	}
	
	public static Map<String, List<String>> getPermissionsMap(){
		if (permissionsMap == null){
			//If this is the first time accessing permissions, populate the map
			permissionsMap = new HashMap<String, List<String>>(values().length);
			for (AccessControlRole role: values()){
				List<CDRProperty> rolePermissions = role.getPermissions();
				List<String> permissions = new ArrayList<String>(rolePermissions.size());
				for (CDRProperty permission: rolePermissions){
					permissions.add(permission.getPredicate());
				}
				permissionsMap.put(role.getUri().toString(), Collections.unmodifiableList(permissions));
			}
			// Make the map unmodifiable
			permissionsMap = Collections.unmodifiableMap(permissionsMap);
		}
		return permissionsMap;
	}
	
	public static List<String> getRolePermissions(String role){
		Map<String, List<String>> permissionsMap = getPermissionsMap();
		return permissionsMap.get(role);
	}
	
	public static boolean roleExists(String role){
		return getPermissionsMap().containsKey(role);
	}
}
