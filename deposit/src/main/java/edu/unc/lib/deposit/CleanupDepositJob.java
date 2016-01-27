package edu.unc.lib.deposit;

import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

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
			LOG.debug("deleted {}", cFile);
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
		// load a Stages object
		Stages stages = getStages();

		Model m = getWritableModel();

		// clean up staged files according to staging area policy
		TagURIPattern tagPattern = new TagURIPattern();
		Property fileLocation = dprop(m, DepositRelationship.stagingLocation);
		NodeIterator ni = m.listObjectsOfProperty(fileLocation);
		while (ni.hasNext()) {
			RDFNode n = ni.nextNode();
			String stagingLoc = n.asLiteral().getString();
			URI stagingUri = URI.create(stagingLoc);

			// skip any staged files that are not tag: URIs
			// these may be local deposit processing folder files or
			// other files that we cannot handle.
			if(!tagPattern.matches(stagingUri)) continue;

			SharedStagingArea area = stages.findMatchingArea(stagingUri);
			if (area == null) {
				LOG.error("Cannot find staging area for URI: "
						+ stagingUri.toString());
				break;
			}
			if (!area.isConnected()) {
				stages.connect(area.getURI());
			}

			URI storageUri = null;
			try {
				storageUri = area.getStorageURI(stagingUri);
			} catch (StagingException e) {
				LOG.error(
						"Could not resolve storage URI: "
								+ stagingUri.toString(), e);
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
							LOG.debug("deleted {}", parent.toPath());
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

		// Cleanup files and directories specifically requested be cleaned up by an earlier job
		NodeIterator it = m.listObjectsOfProperty(dprop(m, DepositRelationship.cleanupLocation));
		while (it.hasNext()) {
			RDFNode n = it.nextNode();
			String stagingLoc = n.asLiteral().getString();
			URI stagingUri = URI.create(stagingLoc);

			if(!tagPattern.matches(stagingUri)) continue;

			SharedStagingArea area = stages.findMatchingArea(stagingUri);
			if (area == null) {
				LOG.error("Cannot find staging area for URI: " + stagingUri.toString());
				break;
			}
			if (!area.isConnected()) {
				stages.connect(area.getURI());
			}

			URI storageUri = null;
			try {
				storageUri = area.getStorageURI(stagingUri);
			} catch (StagingException e) {
				LOG.error("Could not resolve storage URI: {}", stagingUri.toString(), e);
			}
			CleanupPolicy p = area.getIngestCleanupPolicy();
			switch (p) {
			case DELETE_INGESTED_FILES:
			case DELETE_INGESTED_FILES_EMPTY_FOLDERS:
				File cleanupFile = new File(storageUri);
				if (cleanupFile.exists()) {
					if (cleanupFile.isDirectory()) {
						try {
							FileUtils.deleteDirectory(cleanupFile);
						} catch (IOException e) {
							LOG.error("Cannot delete deposit directory: {}",
									getDepositDirectory().getAbsolutePath(), e);
						}
					} else {
						cleanupFile.delete();
					}
				}
				break;
			default:
				break;
			}
		}

		// delete deposit folder
		try {
			FileUtils.deleteDirectory(getDepositDirectory());
			LOG.debug("deleted {}", getDepositDirectory());
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
