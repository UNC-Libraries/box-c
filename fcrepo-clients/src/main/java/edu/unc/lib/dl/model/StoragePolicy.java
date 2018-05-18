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
package edu.unc.lib.dl.model;

/**
 * Storage policy describing how a datastream should be stored and addressed.
 *
 * @author bbpennel
 *
 */
public enum StoragePolicy {
    INTERNAL("Stored within the repository's internally managed datastore."),
    PROXIED("Managed and served via the repository from an external URI."),
    EXTERNAL("Stored, addressed and managed outside of repository."),
    REDIRECTED("Tracked by repository, requests are redirected to an external URI.");

    private final String description;

    private StoragePolicy(String description) {
        this.description = description;
    }

    /**
     * @return description of the storage policy
     */
    public String getDescription() {
        return description;
    }
}
