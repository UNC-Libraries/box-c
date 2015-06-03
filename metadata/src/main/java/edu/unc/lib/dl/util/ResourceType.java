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
package edu.unc.lib.dl.util;

import static edu.unc.lib.dl.util.ContentModelHelper.Model.AGGREGATE_WORK;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.COLLECTION;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.SIMPLE;

import java.util.Arrays;
import java.util.List;

import edu.unc.lib.dl.util.ContentModelHelper.Model;

public enum ResourceType {
	Collection(1, Arrays.asList(CONTAINER, COLLECTION)),
			Aggregate(3, Arrays.asList(CONTAINER, AGGREGATE_WORK)),
			Folder(2, Arrays.asList(CONTAINER)),
			File(3,  Arrays.asList(SIMPLE));
	
	private int displayOrder;
	private List<Model> contentModels;
	
	ResourceType(int displayOrder, List<Model> contentModels) {
		this.displayOrder = displayOrder;
		this.contentModels = contentModels;
	}
	
	public int getDisplayOrder(){
		return this.displayOrder;
	}
	
	public boolean equals(String name) {
		return this.name().equals(name);
	}
	
	public List<Model> getContentModels() {
		return contentModels;
	}

	public static ResourceType getResourceTypeByContentModels(List<String> contentModels) {
		if (contentModels.contains(COLLECTION.getPID().getURI())) {
			return Collection;
		}
		if (contentModels.contains(AGGREGATE_WORK.getPID().getURI())) {
			return Aggregate;
		}
		if (contentModels.contains(CONTAINER.getPID().getURI())) {
			return Folder;
		}
		if (contentModels.contains(SIMPLE.getPID().getURI())) {
			return File;
		}
		return null;
	}
}
