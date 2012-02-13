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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
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
@RequestMapping(value={"/ingest*", "/ingest"})
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
		uris.put("queuedJobs", contextUrl + "/rest/ingest/queued");
		uris.put("activeJobs", contextUrl + "/rest/ingest/active");
		uris.put("finishedJobs", contextUrl + "/rest/ingest/finished");
		uris.put("failedJobs", contextUrl + "/rest/ingest/failed");
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
		if(begin == null) {
			begin = 0;
		}
		if(end == null) {
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
			job.put("submissionTime", b.getIngestProperties().getSubmissionTime() > 0 ? b.getIngestProperties().getSubmissionTime() : null);
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

	/**
	 * @param ingestProperties
	 * @return
	 */
	private Object getContainerList(IngestProperties ingestProperties) {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for(ContainerPlacement p : ingestProperties.getContainerPlacements().values()) {
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
		if(begin == null) {
			begin = 0;
		}
		if(end == null) {
			end = failedDirs.length;
		}
		result.put("begin", begin);
		result.put("end", end);
		for (int i = begin; i < end; i++) {
			File f = failedDirs[i];
			IngestProperties props;
			Map<String, Object> job = new HashMap<String, Object>();
			job.put("id", f.getName());
			try {
				props = new IngestProperties(f);
				job.put("submitter", props.getSubmitter());
				job.put("submissionTime", props.getSubmissionTime() > 0 ? props.getSubmissionTime() : null);
				job.put("depositPID", props.getOriginalDepositId());
				job.put("message", props.getMessage());
				int c = f.list(new FilenameFilter() {
					@Override
					public boolean accept(File arg0, String arg1) {
						return arg1.endsWith(".foxml");
					}
				}).length;
				job.put("size", c);
				job.put("worked", c);
				long mod = new File(f, "ingested.log").lastModified();
				job.put("startTime",  mod);
				job.put("finishedTime", null);
				job.put("running", false);
				job.put("containerPlacements", getContainerList(props));
			} catch (Exception e1) {
				LOG.error("Unexpected error building ingest properties", e1);
				throw new Error("Unexpected error building ingest properties",e1);
			}
			String error = null;
			try {
				File faillog = new File(f, BatchIngestTask.FAIL_LOG);
				job.put("failedTime", faillog.lastModified());
				BufferedReader reader = new BufferedReader(new FileReader(faillog));
				String line = null;
				StringBuilder stringBuilder = new StringBuilder();
				String ls = System.getProperty("line.separator");
				while ((line = reader.readLine()) != null) {
					stringBuilder.append(line);
					stringBuilder.append(ls);
				}
				error = stringBuilder.toString();
			} catch (IOException e) {
				error = "Cannot read failure log: " + e.getMessage();
			}
			job.put("error", error);
			jobs.add(job);
		}
		return result;
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
		if(begin == null) {
			begin = 0;
		}
		if(end == null) {
			end = finishedDirs.length;
		}
		result.put("begin", begin);
		result.put("end", end);
		for (int i = begin; i < end; i++) {
			File f = finishedDirs[i];
			IngestProperties props;
			Map<String, Object> job = new HashMap<String, Object>();
			job.put("id", f.getName());
			try {
				props = new IngestProperties(f);
				job.put("submitter", props.getSubmitter());
				job.put("submissionTime", props.getSubmissionTime() > 0 ? props.getSubmissionTime() : null);
				job.put("finishedTime", props.getFinishedTime() > 0 ? props.getFinishedTime() : null);
				job.put("depositId", props.getOriginalDepositId());
				job.put("message", props.getMessage());
				int c = f.list(new FilenameFilter() {
					@Override
					public boolean accept(File arg0, String arg1) {
						return arg1.endsWith(".foxml");
					}
				}).length;
				job.put("size", c);
				job.put("worked", c);
				long mod = new File(f, "ingested.log").lastModified();
				job.put("startTime",  mod);
				job.put("finishedTime", props.getFinishedTime());
				job.put("running", false);
				job.put("containerPlacements", getContainerList(props));
			} catch (Exception e1) {
				LOG.error("Unexpected error building ingest properties", e1);
				throw new Error("Unexpected error building ingest properties",e1);
			}
			jobs.add(job);
		}
		return result;
	}
}
