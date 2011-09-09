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
package edu.unc.lib.dl.ingest.sip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.util.ZipFileUtil;

public class METSPackageSIP implements SubmissionInformationPackage {
    private static final Log log = LogFactory.getLog(METSPackageSIP.class);
    public static final String metsLocation = "METS.xml";
    public static final String metsLocation2 = "mets.xml";
    private boolean discardDataFilesOnDestroy = true;
    private Agent owner = null;
    private File metsFile = null;
    private File tempSIPDir = null;
    private String containerPath = null;
    private boolean allowIndexing = true;
    private PreIngestEventLogger preIngestEvents = new PreIngestEventLogger();

    public boolean isAllowIndexing() {
        return allowIndexing;
    }

    public void setAllowIndexing(boolean allowIndexing) {
        this.allowIndexing = allowIndexing;
    }

    public PreIngestEventLogger getPreIngestEventLogger() {
	return this.preIngestEvents;
    }

    public METSPackageSIP(String containerPath, File sip, Agent owner, boolean isZIP) throws IOException {
	if (isZIP) {
	    this.tempSIPDir = ZipFileUtil.unzipToTemp(sip);
	    metsFile = new File(tempSIPDir, metsLocation);
	    if (!metsFile.exists()) {
		metsFile = new File(tempSIPDir, metsLocation2);
	    }
	    if (!metsFile.exists()) {
		throw new IOException("SIP must contain a METS file named METS.xml or mets.xml");
	    }
	} else { // NOT A ZIP, JUST METS FILE
	    metsFile = sip;
	    if (!metsFile.exists()) {
		throw new IOException("METS file " + metsFile.getPath() + " not found.");
	    }
	}
	this.containerPath = containerPath;
	this.owner = owner;
    }

    public METSPackageSIP(String containerPath, InputStream sip, Agent owner, boolean isZIP) throws IOException {
	if (isZIP) {
	    this.tempSIPDir = ZipFileUtil.unzipToTemp(sip);
	    metsFile = new File(tempSIPDir, metsLocation);
	    if (!metsFile.exists()) {
		metsFile = new File(tempSIPDir, metsLocation2);
	    }
	    if (!metsFile.exists()) {
		throw new IOException("SIP must contain a METS file named METS.xml or mets.xml");
	    }
	} else { // NOT A ZIP, JUST METS Stream
	    metsFile = File.createTempFile("metscopy-", ".xml");
	    OutputStream os = new BufferedOutputStream(new FileOutputStream(metsFile));
	    try {
		byte[] buf = new byte[4096];
		int len;
		while ((len = sip.read(buf)) != -1) {
		    os.write(buf, 0, len);
		}
	    } finally {
		sip.close();
		os.flush();
		os.close();
	    }
	    if (!metsFile.exists()) {
		throw new IOException("METS file " + metsFile.getPath() + " not found.");
	    }
	}
	this.containerPath = containerPath;
	this.owner = owner;
    }

    public File getMetsFile() {
	return metsFile;
    }

    public File getTempSIPDir() {
	return tempSIPDir;
    }

    public List<File> getDataFiles() {
	List<File> result = null;
	if(this.tempSIPDir != null) {
	    result = ZipFileUtil.getFilesInDir(this.tempSIPDir);
	    File mets = null;
	    for(File f : result) {
		if(this.getMetsFile().equals(f)) {
		    mets = f;
		}
	    }
	    if(mets != null) {
		result.remove(mets);
	    }
	} else {
	    result = new ArrayList<File>();
	}
	return result;
    }

    public String getContainerPath() {
	return containerPath;
    }

    public Agent getOwner() {
	return owner;
    }

    public File getFileForLocator(String url) throws IOException {
	if(getTempSIPDir() != null) {
	    return ZipFileUtil.getFileForUrl(url, getTempSIPDir());
	} else {
	    throw new IOException("There are no zipped files associated with this METS");
	}
    }

    public void setDiscardDataFilesOnDestroy(boolean discard) {
	this.discardDataFilesOnDestroy = discard;
    }

    @Override
    protected void finalize() throws Throwable {
	super.finalize();
	this.destroy();
    }

    public void destroy() {
	log.debug("destroy called");
	// cleanup *any* remaining files
	if (discardDataFilesOnDestroy) {
	    if(tempSIPDir != null && tempSIPDir.exists()) {
		ZipFileUtil.deleteDir(tempSIPDir);
	    }
	    if (metsFile != null && metsFile.exists()) {
		metsFile.delete();
	    }
	}
    }

}
