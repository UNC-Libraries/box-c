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
package edu.unc.lib.boxc.search.api.models;

/**
 * A single tier entry in the content path of an object
 * @author bbpennel
 * @date Mar 18, 2015
 */
public class ObjectPathEntry {

    private String pid;
    private String name;
    private boolean isContainer;
    private String collectionId;

    public ObjectPathEntry(String pid, String name, boolean isContainer, String collectionId) {
        this.pid = pid;
        this.name = name;
        this.isContainer = isContainer;
        this.collectionId = collectionId;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isContainer() {
        return isContainer;
    }

    public void setContainer(boolean isContainer) {
        this.isContainer = isContainer;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String id) {
        this.collectionId = id;
    }

}
