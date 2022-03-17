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
package edu.unc.lib.boxc.model.api;

import java.util.List;

import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;

/**
 * Resource Types
 * @author bbpennel
 *
 */
public enum ResourceType {
    AdminUnit(Cdr.AdminUnit),
    Collection(Cdr.Collection),
    Folder(Cdr.Folder),
    Work(Cdr.Work),
    File(Cdr.FileObject),
    Binary(Fcrepo4Repository.Binary),
    DepositRecord(Cdr.DepositRecord),
    ContentRoot(Cdr.ContentRoot);

    private String uri;
    private Resource resource;

    ResourceType(Resource resource) {
        this.resource = resource;
        this.uri = resource.getURI();
    }

    /**
     * @return the resource type as a String URI
     */
    public String getUri() {
        return this.uri;
    }

    /**
     * @return the resource type represented as an RDF resource
     */
    public Resource getResource() {
        return resource;
    }

    public boolean equals(String name) {
        return this.name().equals(name);
    }

    /**
     * @param name
     * @return Name of this resource type matches the provided value
     */
    public boolean nameEquals(String name) {
        return this.name().equals(name);
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
}
