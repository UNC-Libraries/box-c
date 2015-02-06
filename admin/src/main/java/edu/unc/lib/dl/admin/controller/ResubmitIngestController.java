package edu.unc.lib.dl.admin.controller;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.dl.acl.util.AccessGroupConstants;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;
import edu.unc.lib.dl.util.MetsHeaderScanner;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;

@Controller
public class ResubmitIngestController {
	
	private static final Logger log = LoggerFactory.getLogger(ResubmitIngestController.class);
	
	@Autowired
	private DepositStatusFactory depositStatusFactory;
	
	@Autowired
	private File depositsDirectory;
	
	@RequestMapping(value = "ingest/{pid}/resubmit", method = RequestMethod.POST)
	public @ResponseBody
	Map<String, ? extends Object> handleResubmit(@PathVariable("pid") String pid, @RequestParam("file") MultipartFile uploadedFile, HttpServletRequest request, HttpServletResponse response) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		PID depositPid = new PID(pid);
		
		
		// Try to retrieve the deposit status
		
		Map<String, String> status = depositStatusFactory.get(depositPid.getUUID());
		
		if (status == null || status.isEmpty()) {
			response.setStatus(400);
			result.put("error", "Couldn't find the deposit.");
			return result;
		}
		
		
		// Check that the current user is either an admin or was the depositor
		
		AccessGroupSet groups = GroupsThreadStore.getGroups();
		String depositorName = status.get(DepositField.depositorName.name());
		
		if (!(groups.contains(AccessGroupConstants.ADMIN_GROUP) || depositorName.equals(GroupsThreadStore.getUsername()))) {
			response.setStatus(401);
			return result;
		}
		
		
		// Check that the deposit is in the failed state
		
		String state = status.get(DepositField.state.name());
		
		if (state == null || !state.equals(DepositState.failed.name())) {
			response.setStatus(400);
			result.put("error", "The deposit isn't in the failed state.");
			return result;
		}
		
		
		// Create the resubmit deposit directory
		
		String resubmitDirName;
		
		try {
			resubmitDirName = createResubmitDirectory();
		} catch (IOException e) {
			response.setStatus(500);
			result.put("error", "Couldn't save the resubmitted ingest package.");
			return result;
		}
		
		File resubmitDir = new File(depositsDirectory, resubmitDirName);
		File resubmitDataDir = new File(resubmitDir, DepositConstants.DATA_DIR);
		
		
		// Transfer the uploaded file to the resubmit data directory
		
		File resubmitPackageFile = new File(resubmitDataDir, uploadedFile.getOriginalFilename());
		
		try {
			uploadedFile.transferTo(resubmitPackageFile);
		} catch (IOException e) {
			response.setStatus(500);
			result.put("error", "Couldn't save the resubmitted ingest package.");
			return result;
		}
		
		
		// Scan the ingest file
		
		MetsHeaderScanner scanner = new MetsHeaderScanner();
		
		try {
			scanner.scan(resubmitPackageFile, uploadedFile.getOriginalFilename());
		} catch (Exception e) {
			response.setStatus(500);
			result.put("error", "Couldn't scan the resubmitted ingest package.");
			return result;
		}
		
		
		// Check that the ingest file has the same deposit pid as the deposit status
		
		PID depositPackageId = scanner.getObjID();
		
		if (depositPackageId == null) {
			response.setStatus(400);
			result.put("error", "The resubmitted ingest package doesn't have an ID.");
			return result;
		}
		
		if (!depositPackageId.equals(depositPid)) {
			response.setStatus(400);
			result.put("error", "The resubmitted ingest package doesn't match the deposit.");
			return result;
		}
		
		
		// Set new deposit status
		
		depositStatusFactory.set(depositPid.getUUID(), DepositField.resubmitDirName, resubmitDirName);
		depositStatusFactory.set(depositPid.getUUID(), DepositField.resubmitFileName, resubmitPackageFile.getName());
		depositStatusFactory.set(depositPid.getUUID(), DepositField.actionRequest, DepositAction.resubmit.name());
		
		// Return result indicating that we've submitted the file for resubmission
		
		response.setStatus(202);
		result.put("pid", pid);

		return result;
		
	}
	
	/**
	 * Create a resubmit directory inside the deposits directory, containing a data directory.
	 *
	 * @return the name of the resubmit directory (relative to the deposits directory)
	 */
	private String createResubmitDirectory() throws IOException {
		File resubmitDir = File.createTempFile(DepositConstants.RESUBMIT_DIR_PREFIX, "", depositsDirectory);
		resubmitDir.delete();
		resubmitDir.mkdir();
		
		File dataDir = new File(resubmitDir, DepositConstants.DATA_DIR);
		dataDir.mkdir();
		
		return resubmitDir.getName();
	}
	
}
