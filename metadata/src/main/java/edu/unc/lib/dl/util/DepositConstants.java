package edu.unc.lib.dl.util;

import edu.unc.lib.dl.fedora.PID;

public class DepositConstants {
    public static final String DESCRIPTION_DIR = "description";
    public static final String JENA_TDB_DIR = "jena-tdb-model";
    public static final String EVENTS_FILE = "events.xml";
    public static final String EVENTS_DIR = "events";
    public static final String DUBLINCORE_DIR = "dc";
    public static final String FOXML_DIR = "foxml";
    public static final String AIPS_DIR = "aips";
    public static final String DATA_DIR = "data";
    public static final String TECHMD_DIR = "techmd";
    public static final String RESUBMIT_DIR_PREFIX = "resubmit-";
    public static final String RESUBMIT_BACKUP_DIR = "resubmit-backup";
    public static final String FILE_LOCATOR_URI = "http://cdr.lib.unc.edu/schema/bag#locator";

    private DepositConstants() {

    }

    /**
     * Answers the object's PID for a given metadata file within a tag
     * directory.
     * 
     * @param path
     *            the file path, relative or absolute, e.g.
     *            description/65a631a3-8ec0-4233-a4d7-99c6190c875f.xml
     * @return the PID
     */
    public static PID getPIDForTagFile(String path) {
        String uuid = path.substring(path.lastIndexOf('/') + 1,
                path.lastIndexOf('.'));
        return new PID("uuid:" + uuid);
    }
}
