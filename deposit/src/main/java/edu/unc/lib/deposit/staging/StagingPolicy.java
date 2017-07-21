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
package edu.unc.lib.deposit.staging;

/**
 * Staging policy for a single staging location
 * 
 * @author bbpennel
 *
 */
public class StagingPolicy {
    public static enum CleanupPolicy {
        DELETE_INGESTED_FILES,
        DO_NOTHING,
        DELETE_INGESTED_FILES_EMPTY_FOLDERS
    }

    private String path;
    private String name;
    private CleanupPolicy cleanupPolicy;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CleanupPolicy getCleanupPolicy() {
        return cleanupPolicy;
    }

    public void setCleanupPolicy(CleanupPolicy cleanupPolicy) {
        this.cleanupPolicy = cleanupPolicy;
    }
}
