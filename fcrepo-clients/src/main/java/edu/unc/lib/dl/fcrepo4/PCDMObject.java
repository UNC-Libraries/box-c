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

import java.util.List;

/**
 * Repository object based on PCDM model, retrieves data and performs actions
 * with PCDM awareness. 
 * 
 * @author bbpennel
 *
 */
public interface PCDMObject {
	public String getMemberPath(String memberUuid);
	
	public String getMembersPath();
	
	public String addRelatedObject();
	
	public String addFile();
	
	public String addMember();
	
	public void move();
	
	public void delete();
	
	public List<PCDMObject> getMembers();
	
	public List<?> getFiles();
	
	public List<?> getRelatedObjects();
}
