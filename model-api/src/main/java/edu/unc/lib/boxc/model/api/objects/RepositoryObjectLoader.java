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
package edu.unc.lib.boxc.model.api.objects;

import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * @author bbpennel
 */
public interface RepositoryObjectLoader {

    AdminUnit getAdminUnit(PID pid);

    CollectionObject getCollectionObject(PID pid);

    ContentRootObject getContentRootObject(PID pid);

    FolderObject getFolderObject(PID pid);

    WorkObject getWorkObject(PID pid);

    FileObject getFileObject(PID pid);

    BinaryObject getBinaryObject(PID pid);

    DepositRecord getDepositRecord(PID pid);

    Tombstone getTombstone(PID pid);

    RepositoryObject getRepositoryObject(PID pid);

    /**
     * Clear any cache entry for the provided pid
     * @param pid
     */
    void invalidate(PID pid);
}