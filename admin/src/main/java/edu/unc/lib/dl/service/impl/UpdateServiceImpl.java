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
package edu.unc.lib.dl.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.schema.MoveObjectRequest;
import edu.unc.lib.dl.schema.MoveObjectResponse;
import edu.unc.lib.dl.schema.SingleUpdateFile;
import edu.unc.lib.dl.schema.UpdateIngestObject;
import edu.unc.lib.dl.service.UpdateService;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.Constants;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class UpdateServiceImpl implements UpdateService {
	protected final Log logger = LogFactory.getLog(getClass());
	private DigitalObjectManager digitalObjectManager;
	private TripleStoreQueryService tripleStoreQueryService;

	public UpdateIngestObject update(UpdateIngestObject request) {

		logger.debug("In UpdateServiceImpl.update");

		try {
			String agent = request.getAdminOnyen();

			PID pid = new PID(request.getPid());

			// see if metadata is populated
			if ((request.getMetadata() != null) && (request.getMetadata().length > 0)) {
				logger.debug("metadata bytes: " + request.getMetadata().length);

				File file = File.createTempFile("test", "ing");
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(request.getMetadata());
				fos.close();

				digitalObjectManager.updateDescription(pid, file, request.getMetadataChecksum(), agent,
						"Updated through UI");

				request.setMessage(Constants.SUCCESS);
			}

			if (request.getFiles() != null) {
				for (SingleUpdateFile singleUpdateFile : request.getFiles()) {

					if ((singleUpdateFile != null) && (singleUpdateFile.getFile() != null)) {

						File file = new File(singleUpdateFile.getFile());

						logger.debug("file name: " + singleUpdateFile.getFileName());
						logger.debug("file type: " + singleUpdateFile.getMimetype());
						logger.debug("file checksum: '" + singleUpdateFile.getChecksum() + "'");

						if (!notNull(singleUpdateFile.getChecksum())) {
							singleUpdateFile.setChecksum(null);
						}

						digitalObjectManager.updateSourceData(pid, singleUpdateFile.getDsId(), file,
								singleUpdateFile.getChecksum(), singleUpdateFile.getLabel(), singleUpdateFile.getMimetype(),
								agent, "Updated through UI");

						request.setMessage(Constants.SUCCESS);
					}
				}
			}

		} catch (IOException e) {
			request.setMessage(Constants.FAILURE);

			e.printStackTrace();

			return request;
		} catch (IngestException e) {
			request.setMessage(Constants.FAILURE);

			e.printStackTrace();
			return request;
		} catch (Exception e) {
			request.setMessage(Constants.FAILURE);

			e.printStackTrace();
			return request;
		}

		request.getFiles().clear();
		request.setMetadata(new byte[0]);
		request.setPid(null);
		request.setMetadataChecksum(null);

		return request;
	}

	public MoveObjectResponse moveObject(MoveObjectRequest request) {

		MoveObjectResponse response = new MoveObjectResponse();

		// Create and start the thread
		MoveObjectThread thread = new MoveObjectThread();
		thread.setMoveObjectRequest(request);
		thread.start();

		response.setMessage(Constants.IN_PROGRESS_THREADED);

		return response;
	}

	private boolean notNull(String value) {
		if ((value == null) || (value.equals(""))) {
			return false;
		}

		return true;
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	class MoveObjectThread extends Thread {
		MoveObjectRequest moveObjectRequest;

		@Override
		public void run() {
			try {

				String agent = moveObjectRequest.getAdminOnyen();

				PID parent = new PID(moveObjectRequest.getParent());

				List<PID> pids = new ArrayList<PID>(moveObjectRequest.getChildren().size());

				for (String child : moveObjectRequest.getChildren()) {
					PID childPid = new PID(child);
					pids.add(childPid);
				}

				digitalObjectManager.move(pids, parent, agent, "Moved through UI");

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void setMoveObjectRequest(MoveObjectRequest moveObjectRequest) {
			this.moveObjectRequest = moveObjectRequest;
		}
	}
}
