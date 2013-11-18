/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.cdr.services.BatchIngestService;
import edu.unc.lib.dl.services.BatchIngestTask;
import edu.unc.lib.dl.util.ContainerPlacement;
import edu.unc.lib.dl.util.IngestProperties;

/**
 * @author Gregory Jansen
 * 
 */
@Controller
@RequestMapping(value = { "/status/ingest*", "/status/ingest" })
public class IngestServiceRestController extends AbstractServiceConductorRestController {
	private static final Logger LOG = LoggerFactory.getLogger(IngestServiceRestController.class);

	@Resource
	protected BatchIngestService batchIngestService = null;

	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getIngestInfo() {
		Map<String, Object> result = new HashMap<String, Object>();
		LOG.debug("getIngestInfo()");
		addServiceConductorInfo(result, this.batchIngestService);
		result.put("queuedJobs", this.batchIngestService.getQueuedJobCount());
		result.put("failedJobs", this.batchIngestService.getFailedJobCount());
		result.put("finishedJobs", this.batchIngestService.getFinishedJobCount());
		Map<String, Object> uris = new HashMap<String, Object>();
		result.put("uris", uris);
		uris.put("queuedJobs", "/api/status/ingest/queued");
		uris.put("activeJobs", "/api/status/ingest/active");
		uris.put("finishedJobs", "/api/status/ingest/finished");
		uris.put("failedJobs", "/api/status/ingest/failed");
		return result;
	}

	@RequestMapping(value = { "queued" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getIngestQueued(@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		Map<String, Object> result = new HashMap<String, Object>();
		LOG.debug("getIngestQueued()");
		List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
		result.put("jobs", jobs);
		List<BatchIngestTask> queued = this.batchIngestService.getQueuedJobs();
		result.put("count", queued.size());
		if (begin == null) {
			begin = 0;
		}
		if (end == null) {
			end = queued.size();
		}
		result.put("begin", begin);
		result.put("end", end);
		if (begin != null && end != null) {
			queued = queued.subList(begin, end);
		}
		for (BatchIngestTask b : queued) {
			Map<String, Object> job = new HashMap<String, Object>();
			addBatchIngestTaskInfo(job, b);
			jobs.add(job);
		}
		return result;
	}

	@RequestMapping(value = { "active" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getIngestActive() {
		Map<String, Object> result = new HashMap<String, Object>();
		LOG.debug("getIngestActive()");
		List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
		result.put("jobs", jobs);
		Set<BatchIngestTask> active = this.batchIngestService.getActiveJobs();
		result.put("count", active.size());
		for (BatchIngestTask b : active) {
			Map<String, Object> job = new HashMap<String, Object>();
			addBatchIngestTaskInfo(job, b);
			jobs.add(job);
		}
		return result;
	}

	private void addBatchIngestTaskInfo(Map<String, Object> job, BatchIngestTask b) {
		try {
			job.put("id", b.getBaseDir().getName());
			job.put("submitter", b.getIngestProperties().getSubmitter());
			job.put("submissionTime", b.getIngestProperties().getSubmissionTime() > 0 ? b.getIngestProperties()
					.getSubmissionTime() : null);
			job.put("depositPID", b.getIngestProperties().getOriginalDepositId());
			job.put("message", b.getIngestProperties().getMessage());
			job.put("size", b.getFoxmlFiles().length);
			job.put("worked", b.getIngestedCount());
			job.put("startTime", b.getStartTime() > 0 ? b.getStartTime() : null);
			job.put("finishedTime", b.getFinishedTime() > 0 ? b.getFinishedTime() : null);
			job.put("running", b.isRunning());
			job.put("containerPlacements", getContainerList(b.getIngestProperties()));
		} catch (Exception e) {
			job.put("error", e.getMessage());
		}
	}

	private void addBatchIngestTaskDetails(Map<String, Object> job, BatchIngestTask b) {
		addBatchIngestTaskInfo(job, b);
		try {
			// Extract the list of files ingested so far
			job.put("ingestedFiles", this.getIngestedFiles(b.getIngestLog()));
		} catch (Exception e) {
			job.put("error", e.getMessage());
		}
	}

	private List<Map<String, String>> getIngestedFiles(File ingestLogFile) throws IOException {
		String ingestedLog = FileUtils.readFileToString(ingestLogFile);
		String[] ingestLines = ingestedLog.split("\\n");
		List<Map<String, String>> ingestedFiles = new ArrayList<Map<String, String>>(ingestLines.length);
		for (String ingestedLine : ingestLines) {
			String[] ingestProperties = ingestedLine.split("\\t");
			Map<String, String> ingestedFile = new HashMap<String, String>();
			ingestedFile.put("pid", ingestProperties[0]);
			ingestedFile.put("file", ingestProperties[1]);
			if (ingestProperties.length > 2)
				ingestedFile.put("label", ingestProperties[2]);
			if (ingestProperties.length > 3)
				ingestedFile.put("time", ingestProperties[3]);
			ingestedFiles.add(ingestedFile);
		}
		return ingestedFiles;
	}

	/**
	 * @param ingestProperties
	 * @return
	 */
	private Object getContainerList(IngestProperties ingestProperties) {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (ContainerPlacement p : ingestProperties.getContainerPlacements().values()) {
			Map<String, Object> place = new HashMap<String, Object>();
			place.put("submittedLabel", p.label);
			place.put("containerPID", p.parentPID.getPid());
			place.put("designatedOrder", p.designatedOrder);
			place.put("submittedOrder", p.sipOrder);
			place.put("submittedPID", p.pid.getPid());
			result.add(place);
		}
		return result;
	}

	@RequestMapping(value = { "failed" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getIngestFailed(@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		Map<String, Object> result = new HashMap<String, Object>();
		LOG.debug("getIngestFailed()");
		List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
		result.put("jobs", jobs);
		File[] failedDirs = this.batchIngestService.getBatchIngestQueue().getFailedDirectories();
		result.put("count", failedDirs.length);
		if (begin == null) {
			begin = 0;
		}
		if (end == null) {
			end = failedDirs.length;
		}
		result.put("begin", begin);
		result.put("end", end);
		for (int i = begin; i < end; i++) {
			File f = failedDirs[i];
			jobs.add(getFailedJob(f));
		}
		return result;
	}

	private Map<String, Object> getFailedJob(File f) {
		IngestProperties props;
		Map<String, Object> job = new HashMap<String, Object>();
		job.put("id", f.getName());
		try {
			props = new IngestProperties(f);
			job.put("submitter", props.getSubmitter());
			job.put("submissionTime", props.getSubmissionTime() > 0 ? props.getSubmissionTime() : null);
			job.put("depositPID", props.getOriginalDepositId());
			job.put("message", props.getMessage());
			// Determine the total number of files that are being ingested
			int c = getFOXMLCount(f);
			job.put("size", c);
			// Scan the ingest log file to count the number of entries
			BufferedReader r = null;
			String lastLine = null;
			int countLines = 0;
			try {
				r = new BufferedReader(new FileReader(new File(f, BatchIngestTask.INGEST_LOG)));
				for (String line = r.readLine(); line != null; line = r.readLine()) {
					lastLine = line;
					countLines++;
				}
			} finally {
				if (r != null) {
					try {
						r.close();
					} catch (IOException ignored) {
					}
				}
			}
			// Adjust the count of completed files depending on if the last file actually completed or is a container
			// update
			if (lastLine != null) {
				String[] lastarray = lastLine.split("\\t");
				if (lastarray.length > 1 && BatchIngestTask.CONTAINER_UPDATED_CODE.equals(lastarray[1])) {
					job.put("worked", c);
				} else {
					if (lastarray.length > 2) {
						job.put("worked", countLines);
					} else {
						job.put("worked", countLines - 1);
					}
				}
			} else {
				job.put("worked", countLines);
			}
			job.put("startTime", props.getStartTime());
			job.put("running", false);
			job.put("containerPlacements", getContainerList(props));
		} catch (Exception e1) {
			LOG.error("Unexpected error building ingest properties", e1);
			throw new Error("Unexpected error building ingest properties", e1);
		}

		return job;
	}

	/**
	 * Expanded job information for failed jobs, adds in details about which files were processed and a description of
	 * the error which triggered the failure
	 * 
	 * @param f
	 * @return
	 */
	private Map<String, Object> getFailedJobDetails(File f) {
		IngestProperties props;
		Map<String, Object> job = new HashMap<String, Object>();
		job.put("id", f.getName());
		try {
			props = new IngestProperties(f);
			job.put("submitter", props.getSubmitter());
			job.put("submissionTime", props.getSubmissionTime() > 0 ? props.getSubmissionTime() : null);
			job.put("depositPID", props.getOriginalDepositId());
			job.put("message", props.getMessage());
			// Determine the total number of files that are being ingested
			job.put("size", getFOXMLCount(f));

			// Get the list of files that ingested and the count of files that succeeded.
			List<Map<String, String>> ingestedFiles = getIngestedFiles(new File(f, BatchIngestTask.INGEST_LOG));
			job.put("ingestedFiles", ingestedFiles);
			if (ingestedFiles.size() > 0) {
				Map<String, String> lastFile = ingestedFiles.get(ingestedFiles.size() - 1);
				// Last file didn't completely ingest, or last file is a container update message
				if (lastFile.size() == 3 || BatchIngestTask.CONTAINER_UPDATED_CODE.equals(lastFile.get("file"))) {
					job.put("worked", ingestedFiles.size() - 1);
				} else {
					// The rare case where the last file did completely ingest, but then the ingest failed
					job.put("worked", ingestedFiles.size());
				}
			}

			job.put("startTime", props.getStartTime());
			job.put("running", false);
			job.put("containerPlacements", getContainerList(props));
		} catch (Exception e1) {
			LOG.error("Unexpected error building ingest properties", e1);
			throw new Error("Unexpected error building ingest properties", e1);
		}
		// Get the error that caused this ingest to fail
		String error = null;
		File faillog = new File(f, BatchIngestTask.FAIL_LOG);
		job.put("failedTime", faillog.lastModified());
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(faillog));
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}
		} catch (IOException e) {
			error = "Cannot read failure log: " + e.getMessage();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignored) {
				}
			}
		}
		error = stringBuilder.toString();
		job.put("error", error);
		return job;
	}

	@RequestMapping(value = { "finished" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getIngestFinished(@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		Map<String, Object> result = new HashMap<String, Object>();
		LOG.debug("getIngestFinished()");
		List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
		result.put("jobs", jobs);
		File[] finishedDirs = this.batchIngestService.getBatchIngestQueue().getFinishedDirectories();
		result.put("count", finishedDirs.length);
		if (begin == null) {
			begin = 0;
		}
		if (end == null) {
			end = finishedDirs.length;
		}
		result.put("begin", begin);
		result.put("end", end);
		for (int i = begin; i < end; i++) {
			File f = finishedDirs[i];
			jobs.add(this.getFinishedJob(f));
		}
		return result;
	}

	private Map<String, Object> getFinishedJobDetails(File f) {
		IngestProperties props;
		Map<String, Object> job = new HashMap<String, Object>();
		job.put("id", f.getName());
		try {
			props = new IngestProperties(f);
			job.put("submitter", props.getSubmitter());
			job.put("submissionTime", props.getSubmissionTime() > 0 ? props.getSubmissionTime() : null);
			job.put("startTime", props.getStartTime() > 0 ? props.getStartTime() : null);
			job.put("finishedTime", props.getFinishedTime() > 0 ? props.getFinishedTime() : null);
			job.put("depositId", props.getOriginalDepositId());
			job.put("message", props.getMessage());
			// Get the list of files which were ingested
			List<Map<String, String>> ingestedFiles = getIngestedFiles(new File(f, BatchIngestTask.INGEST_LOG));
			// Ingested counts exclude the container update line
			job.put("size", ingestedFiles.size() - 1);
			job.put("worked", ingestedFiles.size() - 1);
			job.put("ingestedFiles", ingestedFiles);

			job.put("running", false);
			job.put("containerPlacements", getContainerList(props));
		} catch (Exception e1) {
			LOG.error("Unexpected error building ingest properties", e1);
			throw new Error("Unexpected error building ingest properties", e1);
		}
		return job;
	}

	private Map<String, Object> getFinishedJob(File f) {
		IngestProperties props;
		Map<String, Object> job = new HashMap<String, Object>();
		job.put("id", f.getName());
		try {
			props = new IngestProperties(f);
			job.put("submitter", props.getSubmitter());
			job.put("submissionTime", props.getSubmissionTime() > 0 ? props.getSubmissionTime() : null);
			job.put("startTime", props.getStartTime() > 0 ? props.getStartTime() : null);
			job.put("finishedTime", props.getFinishedTime() > 0 ? props.getFinishedTime() : null);
			job.put("depositId", props.getOriginalDepositId());
			job.put("message", props.getMessage());
			// Get count of the files which were ingested
			int c = getFOXMLCount(f);
			job.put("size", c);
			job.put("worked", c);
			job.put("running", false);
			job.put("containerPlacements", getContainerList(props));
		} catch (Exception e1) {
			LOG.error("Unexpected error building ingest properties", e1);
			throw new Error("Unexpected error building ingest properties", e1);
		}
		return job;
	}

	/**
	 * Find and return the details of a specific ingest job by id, regardless of status
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = { "job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getJobDetails(@PathVariable("id") String id) {
		// Search queued jobs
		List<BatchIngestTask> queued = this.batchIngestService.getQueuedJobs();
		for (BatchIngestTask task : queued) {
			if (task.getBaseDir().getName().equals(id)) {
				Map<String, Object> job = new HashMap<String, Object>();
				this.addBatchIngestTaskDetails(job, task);
				job.put("status", "queued");
				return job;
			}
		}
		// Search active jobs
		Set<BatchIngestTask> active = this.batchIngestService.getActiveJobs();
		for (BatchIngestTask task : active) {
			if (task.getBaseDir().getName().equals(id)) {
				Map<String, Object> job = new HashMap<String, Object>();
				this.addBatchIngestTaskDetails(job, task);
				job.put("status", "active");
				return job;
			}
		}
		// Search failed jobs
		File[] failedDirs = this.batchIngestService.getBatchIngestQueue().getFailedDirectories();
		for (File dir : failedDirs) {
			if (dir.getName().equals(id)) {
				Map<String, Object> job = this.getFailedJobDetails(dir);
				job.put("status", "failed");
				return job;
			}
		}
		// Search finished jobs
		File[] finishedDirs = this.batchIngestService.getBatchIngestQueue().getFinishedDirectories();
		for (File dir : finishedDirs) {
			if (dir.getName().equals(id)) {
				Map<String, Object> job = this.getFinishedJobDetails(dir);
				job.put("status", "finished");
				return job;
			}
		}
		return null;
	}

	private int getFOXMLCount(File foxmlPath) {
		try {
			int c = foxmlPath.list(new FilenameFilter() {
				@Override
				public boolean accept(File arg0, String arg1) {
					return arg1.endsWith(".foxml");
				}
			}).length;
			return c;
		} catch (NullPointerException ignored) {
		}
		return 0;
	}
}
