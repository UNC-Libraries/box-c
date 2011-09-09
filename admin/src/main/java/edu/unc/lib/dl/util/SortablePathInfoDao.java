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

import edu.unc.lib.dl.schema.PathInfoDao;

public class SortablePathInfoDao implements Comparable<SortablePathInfoDao> {
	private PathInfoDao internal;

	public void setPathInfoDao(PathInfoDao input) {
		internal = input;
	}

	public PathInfoDao getPathInfoDao() {
		return internal;
	}

	public int compareTo(SortablePathInfoDao child) {
		int result = internal.getLabel().compareTo(child.getPathInfoDao().getLabel());
		return result == 0 ? internal.getPid().compareTo(child.getPathInfoDao().getPid())
				: result;
	}
}