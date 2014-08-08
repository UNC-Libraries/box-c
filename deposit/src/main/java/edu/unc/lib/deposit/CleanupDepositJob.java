package edu.unc.lib.deposit;

import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.staging.CleanupPolicy;
import edu.unc.lib.staging.FileResolver;
import edu.unc.lib.staging.SharedStagingArea;
import edu.unc.lib.staging.Stages;
import edu.unc.lib.staging.StagingException;

/**
 * This job deletes the deposit's processing folder and sets all 
 * Redis keys to expire after a configurable delay. It also may delete 
 * staging files, surrounding folders, or high-level deposit staging folders
 * according to a policy specific to the staging area. 
 * 
 * @author count0
 *
 */
public class CleanupDepositJob extends AbstractDepositJob implements Runnable {
	private static final Logger LOG = LoggerFactory
			.getLogger(CleanupDepositJob.class);

	private String stageConfigURL = null;

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
		URL fileUri;
		try {
			fileUri = new URL(URLDecoder.decode(uri.toString(), "utf-8"));
			String path = fileUri.getPath();
			File cFile = new File(path).getCanonicalFile();
			parent = cFile.getParentFile();
			cFile.delete();
			LOG.debug("deleted {}", path);
		} catch (IOException e) {
			LOG.error("Cannot delete a staged file: " + uri.toString(), e);
		}
		return parent;
	}

	public String getStageConfigURL() {
		return this.stageConfigURL;
	}

	public int getStatusKeysExpireSeconds() {
		return this.statusKeysExpireSeconds;
	}

	@Override
	public void run() {
		// load a Stages object
		Stages stages = null;
		try {
			URL cURL = new URL(this.stageConfigURL);
			File cFile = org.apache.commons.io.FileUtils.toFile(cURL);
			String config = org.apache.commons.io.FileUtils
					.readFileToString(cFile);
			stages = new Stages(config, new FileResolver());
		} catch (Exception e) {
			failJob(e, Type.INGESTION,
					"Failed to read staging areas configuration");
		}

		Model m = ModelFactory.createDefaultModel();
		File modelFile = new File(getDepositDirectory(),
				DepositConstants.MODEL_FILE);
		m.read(modelFile.toURI().toString());

		// FIXME add a check that the staging area is a tag: area
		// clean up staged files according to staging area policy
		Property fileLocation = dprop(m, DepositRelationship.stagingLocation);
		NodeIterator ni = m.listObjectsOfProperty(fileLocation);
		while (ni.hasNext()) {
			RDFNode n = ni.nextNode();
			String stagingLoc = n.asLiteral().getString();
			URI stagingUri = URI.create(stagingLoc);

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
			case DELETE_INGESTED_DEPOSIT_FOLDERS:
			case DELETE_INGESTED_FILES_EMPTY_FOLDERS:
				File parent = deleteFile(storageUri);
				if (parent != null) {
					if (parent.list().length == 0) {
						try {
							Files.delete(parent.toPath());
						} catch (IOException e) {
							LOG.error(
									"Cannot delete an empty staging directory: "
											+ parent.getAbsolutePath(), e);
						}
					}
				}
			}
		}

		// delete project staging folder, if exists/applicable
		String sfuris = getDepositStatus().get(DepositField.stagingFolderURI.name());
		if (sfuris != null && sfuris.trim().length() > 0) {
			SharedStagingArea area = null;
			URI sfuri = null;
			try {
				sfuri = new URI(sfuris);
				area = stages.findMatchingArea(sfuri);
			} catch (URISyntaxException ignore) {
			}
			if (area != null) {
				if (CleanupPolicy.DELETE_INGESTED_DEPOSIT_FOLDERS.equals(area
						.getIngestCleanupPolicy())) {
					if (!area.isConnected()) {
						stages.connect(area.getURI());
					}
					if(area.isConnected()) {
						try {
							URI depositStagingFolderURI = area.getStorageURI(sfuri);
							FileUtils.deleteDirectory(new File(depositStagingFolderURI));
						} catch (StagingException e) {
							LOG.error("Cannot obtain storage location for deposit staging folder URI: "
									+ sfuris, e);
						} catch (IOException e) {
							LOG.error("Cannot delete deposit staging folder: "
									+ sfuris, e);
						}
					}
				}
			} else {
				LOG.error("Cannot find staging area for deposit staging folder for URI: "
						+ sfuris);
			}
		}

		// delete deposit folder
		try {
			FileUtils.deleteDirectory(getDepositDirectory());
		} catch (IOException e) {
			LOG.error("Cannot delete deposit directory: "
					+ getDepositDirectory().getAbsolutePath(), e);
		}

		// expire deposit Redis keys
		getDepositStatusFactory().expireKeys(getDepositUUID(),
				this.getStatusKeysExpireSeconds());
		getJobStatusFactory().expireKeys(getDepositUUID(),
				this.getStatusKeysExpireSeconds());
	}

	public void setStageConfigURL(String stageConfigURL) {
		this.stageConfigURL = stageConfigURL;
	}

	public void setStatusKeysExpireSeconds(int seconds) {
		this.statusKeysExpireSeconds = seconds;
	}

}
