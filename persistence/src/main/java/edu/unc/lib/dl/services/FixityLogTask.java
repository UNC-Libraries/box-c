package edu.unc.lib.dl.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.RuntimeJsonMappingException;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.jdom.Element;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PremisEventLogger;

public class FixityLogTask implements Runnable {
	
	private static final Log LOG = LogFactory.getLog(FixityLogTask.class);

	private IRODSAccount irodsAccount = null;
	private IRODSFileSystem irodsFileSystem = null;
	private ManagementClient managementClient = null;
	private String fixityLogCollection = null;

	private IRODSFileFactory fileFactory;
	private IRODSFile queued;
	private IRODSFile finished;
	private IRODSFile failed;

	public IRODSAccount getIrodsAccount() {
		return irodsAccount;
	}

	public void setIrodsAccount(IRODSAccount irodsAccount) {
		this.irodsAccount = irodsAccount;
	}

	public IRODSFileSystem getIrodsFileSystem() {
		return irodsFileSystem;
	}

	public void setIrodsFileSystem(IRODSFileSystem irodsFileSystem) {
		this.irodsFileSystem = irodsFileSystem;
	}

	public ManagementClient getManagementClient() {
		return managementClient;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}
	
	public String getFixityLogCollection() {
		return fixityLogCollection;
	}
	
	public void setFixityLogCollection(String fixityLogCollection) {
		this.fixityLogCollection = fixityLogCollection;
	}

	public void run() {
		
		LOG.debug("Starting fixity log task");

		try {
			
			fileFactory = irodsFileSystem.getIRODSFileFactory(irodsAccount);

			queued = fileFactory.instanceIRODSFile(fixityLogCollection, "queued");
			finished = fileFactory.instanceIRODSFile(fixityLogCollection, "finished");
			failed = fileFactory.instanceIRODSFile(fixityLogCollection, "failed");

			if (!queued.exists() || !queued.isDirectory()) {
				LOG.error("Fixity log collection not found: " + queued.getPath());
				return;
			}
			
			if (!finished.exists() || !finished.isDirectory()) {
				LOG.error("Fixity log collection not found: " + finished.getPath());
				return;
			}
			
			if (!failed.exists() || !failed.isDirectory()) {
				LOG.error("Fixity log collection not found: " + failed.getPath());
				return;
			}
			
			LOG.debug("Listing files in fixity log collection: " + queued.getPath());
			
			for (String name : queued.list()) {
				IRODSFile file = fileFactory.instanceIRODSFile(queued.getPath(), name);
				processLog(file);
			}

		} catch (JargonException e) {
			LOG.error("Error running FixityLogTask", e);
		} catch (IOException e) {
			LOG.error("Error running FixityLogTask", e);
		} finally {
			irodsFileSystem.closeAndEatExceptions();
		}

	}
	
	private void processLog(IRODSFile file) throws IOException, JargonException {
		
		LOG.debug("Processing fixity log file: " + file.getPath());
		
		InputStream input = null;
		boolean error = false;

		try {
			
			input = fileFactory.instanceIRODSFileInputStream(file);

			Iterator<FixityLogEntry> entries = new ObjectMapper().readValues(new JsonFactory().createJsonParser(input), FixityLogEntry.class);
            
			while (entries.hasNext()) {
				processLogEntry(entries.next());
			}
			
	        if (file.renameTo(fileFactory.instanceIRODSFile(finished.getPath(), file.getName())))
	        	LOG.info("Finished processing log file: " + file.getName());
	        else
	        	LOG.error("Couldn't move log file to finished dir: " + file.getName());
			
		} catch (JargonException e) {
			
			LOG.error("Error processing log file: " + file.getName(), e);
			error = true;
			
		} catch (RuntimeJsonMappingException e) {
			
			LOG.error("Error processing log file: " + file.getName(), e);
			error = true;
			
		} catch (RuntimeException e) {
			
			LOG.error("Error processing log file: " + file.getName(), e);
			error = true;
			
		} catch (IOException e) {
			
			LOG.error("Error processing log file: " + file.getName(), e);
			error = true;
			
		} finally {
			
			if (error) {
		        if (file.renameTo(fileFactory.instanceIRODSFile(failed.getPath(), file.getName())))
		        	LOG.info("Failed to process log file: " + file.getName());
		        else
		        	LOG.error("Couldn't move log file to failed dir: " + file.getName());
			}
			
			input.close();
			
		}

	}

	private void processLogEntry(FixityLogEntry entry) throws JargonException {
		
		if (entry.getResult() != FixityLogEntry.Result.OK) {

			PID pid = entry.getPID();
			
			if (pid != null) {
				
				PremisEventLogger premisEventLogger = new PremisEventLogger(null);
				
				Element event = premisEventLogger.logEvent(pid);
				
				// The eventType is a fixity check
				PremisEventLogger.addType(event, PremisEventLogger.Type.FIXITY_CHECK);
				
				// The eventDateTime is the value from the log, not the current time
				PremisEventLogger.addDateTime(event, entry.getTime());

				// Add eventDetailedOutcome, possibly with an eventDetailNote
				String detailNote = null;
				if (entry.getResult() == FixityLogEntry.Result.FAILED)
					detailNote = "The checksum of the replica on " + entry.getResc() + " didn't match the value recorded in the iCAT.";
				else if (entry.getResult() == FixityLogEntry.Result.ERROR)
					detailNote = "An error occurred while attempting to calculate the checksum for the object on " + entry.getResc() + ". (iRODS error: " + entry.getCode() + " " + entry.getError() + ")";
				else if (entry.getResult() == FixityLogEntry.Result.MISSING)
					detailNote = "The replica on " + entry.getResc() + " was not found in the iCAT.";
				PremisEventLogger.addDetailedOutcome(event, entry.getResult().toString(), detailNote, null);
				
				// Add a linkingAgentIdentifier element for this task
				PremisEventLogger.addLinkingAgentIdentifier(event, "Class", this.getClass().getName(), "Software");
				
				// Add a linkingAgentIdentifier element for iRODS (in terms of release version)
				PremisEventLogger.addLinkingAgentIdentifier(event, "iRODS relVersion", entry.getVersion(), "Software");
				
				// Add a linkingObjectIdentifier for the iRODS object path
				PremisEventLogger.addLinkingObjectIdentifier(event, "iRODS object path", entry.getObj(), null);

				// Add a linkingObjectIdentifier for the datastream (if there was one)
				String datastream = entry.getDatastream();
				if (datastream != null)
					PremisEventLogger.addLinkingObjectIdentifier(event, "PID", pid + "/" + datastream, null);
				
				try {
					this.managementClient.writePremisEventsToFedoraObject(premisEventLogger, pid);
				} catch (FedoraException e) {
					LOG.error("Error writing PREMIS event for pid: " + pid, e);
				}
				
			} else {
				
				LOG.info("No PREMIS event added for fixity log entry without pid: " + entry);
				
			}
			
		}
	
	}
		
}
