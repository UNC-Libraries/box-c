package edu.unc.lib.dl.util;

import edu.unc.lib.dl.fedora.PID;

public class BagConstants {
	public static final String DESCRIPTION_DIR = "description";
	public static final String MODEL_FILE = "everything.n3";
	public static final String EVENTS_FILE = "events.xml";
	
	/**
	 * Answers the object's PID for a given metadata file within a tag directory.
	 * @param path the file path, relative or absolute, e.g. description/65a631a3-8ec0-4233-a4d7-99c6190c875f.xml
	 * @return the PID
	 */
	public static PID getPIDForTagFile(String path) {
		String uuid = path.substring(path.lastIndexOf('/')+1, path.lastIndexOf('.')); 
		return new PID("uuid:"+uuid);
	}
}
