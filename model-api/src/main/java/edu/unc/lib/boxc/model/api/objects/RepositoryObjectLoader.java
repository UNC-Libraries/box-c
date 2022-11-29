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