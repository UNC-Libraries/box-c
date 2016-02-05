package edu.unc.lib.deposit;

import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
import edu.unc.lib.staging.CleanupPolicy;
import edu.unc.lib.staging.SharedStagingArea;
import edu.unc.lib.staging.Stages;
import edu.unc.lib.staging.StagingException;
import edu.unc.lib.staging.TagURIPattern;

/**
 * This job deletes the deposit's processing folder and sets all
 * Redis keys to expire after a configurable delay. It also may delete
 * staged files, surrounding folders, and/or high-level deposit staging folders
 * according to a policy specific to the staging area.
 *
 * @author count0
 *
 */
public class CleanupDepositJob extends AbstractDepositJob {
	private static final Logger LOG = LoggerFactory
			.getLogger(CleanupDepositJob.class);

	private Stages stages;

	private int statusKeysExpireSeconds;
	
	private TagURIPattern tagPattern = new TagURIPattern();

	public CleanupDepositJob() {
	}

	public CleanupDepositJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	/**
	 * Delete a file at a URI and returns the parent File object.
	 * @param uri the location to delete
	 * @return parent File object
	 */
	private File deleteFile(URI uri) {
		File parent = null;
		try {
			File cFile = new File(uri.getPath()).getCanonicalFile();
			parent = cFile.getParentFile();
			cFile.delete();
			LOG.info("Deleted file: {}", cFile.getAbsoluteFile());
		} catch (IOException e) {
			LOG.error("Cannot delete a staged file: " + uri.toString(), e);
		}
		return parent;
	}

	public int getStatusKeysExpireSeconds() {
		return this.statusKeysExpireSeconds;
	}

	@Override
	public void runJob() {
		Model m = getWritableModel();
		
		// clean up staged files according to staging area policy
		deleteStagedFiles(m);
		
		// delete files identified for cleanup
		deleteCleanupFiles(m);

		// delete deposit folder
		try {
			FileUtils.deleteDirectory(getDepositDirectory());
			LOG.info("Deleted deposit directory: {}", getDepositDirectory());
		} catch (IOException e) {
			LOG.error("Cannot delete deposit directory: "
					+ getDepositDirectory().getAbsolutePath(), e);
		}

		// destroy the Jena model for this deposit
		this.destroyModel();

		// set this deposit's Redis keys to expire
		getDepositStatusFactory().expireKeys(getDepositUUID(),
				this.getStatusKeysExpireSeconds());
		getJobStatusFactory().expireKeys(getDepositUUID(),
				this.getStatusKeysExpireSeconds());
	}
	
	private void deleteStagedFiles(Model m) {
		Property fileLocation = dprop(m, DepositRelationship.stagingLocation);
		NodeIterator ni = m.listObjectsOfProperty(fileLocation);
		while (ni.hasNext()) {
			RDFNode n = ni.nextNode();
			URI stagingUri = URI.create(n.asLiteral().getString());
			
			SharedStagingArea area = getStorageArea(stagingUri);
			if (area == null) {
				continue;
			}
			
			URI storageUri = null;
			try {
				storageUri = area.getStorageURI(stagingUri);
			} catch (StagingException e) {
				LOG.error("Could not resolve storage URI: {}", stagingUri.toString(), e);
			}
			
			CleanupPolicy p = area.getIngestCleanupPolicy();
			switch (p) {
			case DO_NOTHING:
				break;
			case DELETE_INGESTED_FILES:
				deleteFile(storageUri);
				break;
			case DELETE_INGESTED_FILES_EMPTY_FOLDERS:
				File parent = deleteFile(storageUri);
				if (parent != null) {
					if (parent.list().length == 0) {
						try {
							Files.delete(parent.toPath());
							LOG.info("Deleted parent folder: {}", parent.toPath());
						} catch (IOException e) {
							LOG.error(
									"Cannot delete an empty staging directory: "
											+ parent.getAbsolutePath(), e);
						}
					}
				}
			default:
				break;
			}
		}
	}
	
	// Cleanup files and directories specifically requested be cleaned up by an earlier job
	private void deleteCleanupFiles(Model m) {
		List<String> cleanupPaths = new ArrayList<>();
		
		// Create a list of files that need to be cleaned up
		NodeIterator it = m.listObjectsOfProperty(dprop(m, DepositRelationship.cleanupLocation));
		while (it.hasNext()) {
			RDFNode n = it.nextNode();
			URI cleanupUri = URI.create(n.asLiteral().getString());
			
			SharedStagingArea area = getStorageArea(cleanupUri);
			if (area == null) {
				continue;
			}
			
			URI storageUri = null;
			try {
				storageUri = area.getStorageURI(cleanupUri);
			} catch (StagingException e) {
				LOG.error("Could not resolve storage URI: {}", cleanupUri.toString(), e);
			}
			
			CleanupPolicy p = area.getIngestCleanupPolicy();
			switch (p) {
			case DELETE_INGESTED_FILES:
			case DELETE_INGESTED_FILES_EMPTY_FOLDERS:
				cleanupPaths.add(storageUri.getPath());
				break;
			default:
				break;
			}
		}
		
		// Sort cleanup files so that deepest will be deleted first
		Collections.sort(cleanupPaths, Collections.reverseOrder());
		
		// Perform deletion of cleanup files in order
		for (String pathString : cleanupPaths) {
			File cleanupFile = new File(pathString);
			try {
				if (cleanupFile.exists()) {
					// non-recursive delete for files or folders
					Files.delete(cleanupFile.toPath());
					LOG.info("Deleted cleanup file: {}", cleanupFile.getAbsoluteFile());
				}
			} catch (IOException e) {
				if (cleanupFile.isDirectory()) {
					LOG.warn("Failed to delete cleanup directory {}, it may not be empty",
							pathString);
				} else {
					LOG.error("Failed to delete cleanup file {}", pathString, e);
				}
			}
		}
	}
	
	private SharedStagingArea getStorageArea(URI stagingUri) {
		if(!tagPattern.matches(stagingUri)) return null;

		SharedStagingArea area = stages.findMatchingArea(stagingUri);
		if (area == null) {
			LOG.error("Cannot find staging area for URI: " + stagingUri.toString());
			return null;
		}
		if (!area.isConnected()) {
			stages.connect(area.getURI());
		}
		
		return area;
	}

	public Stages getStages() {
		return stages;
	}

	public void setStages(Stages stages) {
		this.stages = stages;
	}

	public void setStatusKeysExpireSeconds(int seconds) {
		this.statusKeysExpireSeconds = seconds;
	}

}
