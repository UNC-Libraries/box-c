package edu.unc.lib.dl.ingest.sip;

import java.io.File;

import edu.unc.lib.dl.fedora.PID;

public abstract class FileSIP implements SubmissionInformationPackage {
	protected boolean allowIndexing = true;
	protected PID containerPID = null;
	protected File data = null;
	protected String fileLabel = null;
	protected String md5checksum = null;
	protected String mimeType = null;
	protected boolean discardFilesOnDestroy = false;
	protected String suggestedSlug;
	protected boolean inProgress = false;
	protected PreIngestEventLogger preIngestEvents = new PreIngestEventLogger();

	protected FileSIP() {
		
	}
	
	protected FileSIP(PID containerPID, File data, String mimeType, String fileLabel, String md5checksum) {
		this.containerPID = containerPID;
		this.data = data;
		this.mimeType = mimeType;
		this.fileLabel = fileLabel;
		this.md5checksum = md5checksum;
	}
	
	public void destroy() {
		if (this.discardFilesOnDestroy) {
			this.data.delete();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		this.destroy();
	}
	
	public boolean isInProgress() {
		return inProgress;
	}

	public void setInProgress(boolean inProgress) {
		this.inProgress = inProgress;
	}

	public boolean isAllowIndexing() {
		return allowIndexing;
	}

	/**
	 * Tells the repository whether or not to index this object. (Default is yes)
	 * 
	 * @param allowIndexing
	 */
	public void setAllowIndexing(boolean allowIndexing) {
		this.allowIndexing = allowIndexing;
	}

	
	public PID getContainerPID() {
		return containerPID;
	}

	/**
	 * Set the path to the folder that will contain the entire submission.
	 * 
	 * @param containerPath
	 */
	public void setContainerPID(PID containerPID) {
		this.containerPID = containerPID;
	}

	public File getData() {
		return data;
	}

	/**
	 * Set the data file.
	 * 
	 * @param data
	 */
	public void setData(File data) {
		this.data = data;
	}

	public String getFileLabel() {
		return fileLabel;
	}

	/**
	 * Set the label for the data file, usually the original file name.
	 * 
	 * @param fileLabel
	 */
	public void setFileLabel(String fileLabel) {
		this.fileLabel = fileLabel;
	}

	public String getMd5checksum() {
		return md5checksum;
	}

	/**
	 * Optional: set the checksum for the data file.
	 * 
	 * @param md5checksum
	 */
	public void setMd5checksum(String md5checksum) {
		this.md5checksum = md5checksum;
	}

	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Set the IANA MIME-Type of the data file.
	 * 
	 * @param mimeType
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public boolean isDiscardFilesOnDestroy() {
		return discardFilesOnDestroy;
	}

	public void setDiscardFilesOnDestroy(boolean discardFilesOnDestroy) {
		this.discardFilesOnDestroy = discardFilesOnDestroy;
	}

	public String getSuggestedSlug() {
		return suggestedSlug;
	}

	/**
	 * Predetermined slug to use for this item if it is deemed acceptable
	 * 
	 * @param suggestedSlug
	 */
	public void setSuggestedSlug(String suggestedSlug) {
		this.suggestedSlug = suggestedSlug;
	}
	
	public PreIngestEventLogger getPreIngestEventLogger() {
		return this.preIngestEvents;
	}
}
