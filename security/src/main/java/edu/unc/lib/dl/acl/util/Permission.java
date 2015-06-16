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
package edu.unc.lib.dl.acl.util;

import edu.unc.lib.dl.util.ContentModelHelper.DatastreamCategory;

public enum Permission {
	addRemoveContents, editAccessControl, editDescription, moveToTrash, publish,
	purgeForever, viewAdminUI, viewDerivative, viewDescription, viewEmbargoed,
	viewOriginal, changeResourceType;
	
	private Permission() {}
	
	public static Permission getPermission(String permissionName) {
		for (Permission permission: Permission.values()) {
			if (permission.name().equals(permissionName))
				return permission;
		}
		return null;
	}
	
	public static Permission getPermissionByDatastreamCategory(DatastreamCategory category) {
		switch (category) {
			case DERIVATIVE:
				return Permission.viewDerivative;
			case METADATA:
				return Permission.viewDescription;
			case ORIGINAL:
				return Permission.viewOriginal;
			case ADMINISTRATIVE:
				return Permission.viewAdminUI;
		}
		return null;
	}
}