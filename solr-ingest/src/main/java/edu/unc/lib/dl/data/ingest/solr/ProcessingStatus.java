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
package edu.unc.lib.dl.data.ingest.solr;

/**
 * 
 * @author bbpennel
 *
 */
public enum ProcessingStatus {
    ACTIVE("active"), INPROGRESS("inprogress"), BLOCKED("blocked"),
    QUEUED("queued"), FINISHED("finished"), FAILED("failed"), IGNORED("ignored");

    String name;

    ProcessingStatus(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
