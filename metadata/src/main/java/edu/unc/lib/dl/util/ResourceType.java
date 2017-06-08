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

import java.util.List;

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.util.ContentModelHelper.Model;

/**
 * Resource Types
 * @author bbpennel
 *
 */
public enum ResourceType {
    AdminUnit(1, Cdr.AdminUnit.getURI()),
    Collection(2, Cdr.Collection.getURI()),
    Folder(3, Cdr.Folder.getURI()),
    Work(4, Cdr.Work.getURI()),
    File(4, Cdr.FileObject.getURI()),
    DepositRecord(5, Cdr.DepositRecord.getURI()),
    ContentRoot(5, Cdr.ContentRoot.getURI());

    private int displayOrder;
    private String uri;
    private List<Model> contentModels;

    ResourceType(int displayOrder, String uri) {
        this.displayOrder = displayOrder;
        this.uri = uri;
    }

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public boolean equals(String name) {
        return this.name().equals(name);
    }

    public List<Model> getContentModels() {
        return contentModels;
    }

    public static ResourceType getResourceTypeByUri(String uri) {
        for (ResourceType type : values()) {
            if (type.uri.equals(uri)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Gets the resourceType for rdf type uris
     * 
     * @param uris
     * @return
     */
    public static ResourceType getResourceTypeForUris(List<String> uris) {
        for (String uri : uris) {
            ResourceType type = getResourceTypeByUri(uri);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    @Deprecated
    public static ResourceType getResourceTypeByContentModels(List<String> contentModels) {
        return null;
    }
}
