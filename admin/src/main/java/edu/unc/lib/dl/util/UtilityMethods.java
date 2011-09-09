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
package edu.unc.lib.dl.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import edu.unc.lib.dl.schema.Id;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.service.IdService;
import edu.unc.lib.dl.util.Constants;

public class UtilityMethods {
	private String baseHostUrl;
	private IdService idService;
	private String fedoraDataUrl;

	public static void populateIrUrlInfo(IrUrlInfo irUrlInfo,
			HttpServletRequest request) {
		try {

			int prefixLength = getUrlPrefixLength(request.getRequestURI());

			
			irUrlInfo.setDecodedUrl(URLDecoder.decode(request.getRequestURL()
					.toString(), Constants.UTF_8));

			
			irUrlInfo.setFedoraUrl(URLDecoder.decode(request.getRequestURI()
					.substring(prefixLength), Constants.UTF_8));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String params = request.getQueryString();
		if (params != null)
			irUrlInfo.setParameters(params);
		else
			irUrlInfo.setParameters("");

		irUrlInfo.setUri(request.getRequestURI());
		irUrlInfo.setUrl(request.getRequestURL().toString());

	}

	public static void bufferedRead(BufferedInputStream reader,
			BufferedOutputStream writer) throws IOException {
		byte[] buffer = new byte[4096];
		int count = 0;
		while ((count = reader.read(buffer)) >= 0) {
			writer.write(buffer, 0, count);
		}
		writer.flush();
	}

	public static int getUrlPrefixLength(String url) {
		if (url.startsWith(Constants.DATA_PREFIX)) {
			return Constants.DATA_PREFIX.length();
		} else if (url.startsWith(Constants.MOVE_OBJECT_PREFIX)) {
			return Constants.MOVE_OBJECT_PREFIX.length();
		} else if (url.startsWith(Constants.MOVE_TO_PARENT_PREFIX)) {
			return Constants.MOVE_TO_PARENT_PREFIX.length();
		} else if (url.startsWith(Constants.DELETE_OBJECT_PREFIX)) {
			return Constants.DELETE_OBJECT_PREFIX.length();
		} else if (url.startsWith(Constants.UPDATE_OBJECT_PREFIX)) {
			return Constants.UPDATE_OBJECT_PREFIX.length();
		} else if (url.startsWith(Constants.METS_INGEST_PREFIX)) {
			return Constants.METS_INGEST_PREFIX.length();
		} else if (url.startsWith(Constants.SEARCHINDEX_PREFIX)) {
			return Constants.SEARCHINDEX_PREFIX.length();
		} else if (url.startsWith(Constants.SEARCHREMOVE_PREFIX)) {
			return Constants.SEARCHREMOVE_PREFIX.length();
		} else if (url.startsWith(Constants.IMAGE_VIEW_PREFIX)) {
			return Constants.IMAGE_VIEW_PREFIX.length();
		} else if (url.startsWith(Constants.XSL_PREFIX)) {
			return Constants.XSL_PREFIX.length();
		} else if (url.startsWith(Constants.DATASTREAM_REPORT_PREFIX)) {
			return Constants.DATASTREAM_REPORT_PREFIX.length();
		}
		return Constants.IR_PREFIX.length();
	}

	public String getItemInfoUrlFromPid(String entry) {
		// test:85 ->
		// http://localhost/ir/info/gamelan/mss06-15/p01

		StringBuffer url = new StringBuffer(96);

		url.append(baseHostUrl).append(Constants.IR_PREFIX).append(
				idService.getUrlFromPid(entry));

		return url.toString();
	}

	public String getFedoraUrlFromPid(String entry) {
		// test:85 ->
		// /Collections/gamelan/mss06-15/p01

		return idService.getUrlFromPid(entry);
	}
	
	public String getDataUrlFromPid(String entry) {
		// test:85 ->
		// http://localhost/ir/data/gamelan/mss06-15/p01

		StringBuffer url = new StringBuffer(96);

		url.append(baseHostUrl).append(Constants.DATA_PREFIX).append(
				idService.getUrlFromPid(entry));

		return url.toString();
	}

	public String getThumbnailUrl(String thumbnailPid) {
		// test:85/THUMBNAIL ->
		// http://localhost/ir/data/gamelan/mss06-15/p01?ds=THUMBNAIL

		String[] thumbnail = thumbnailPid.split("/");

		StringBuffer url = new StringBuffer(96);

		url.append(baseHostUrl).append(Constants.DATA_PREFIX).append(
				idService.getUrlFromPid(thumbnail[0])).append('?').append(
				Constants.DS_PREFIX).append(thumbnail[1]);

		return url.toString();
	}

	public String getFedoraDataUrlFromIrUrlInfo(IrUrlInfo irUrlInfo) {
		Id id = idService.getId(irUrlInfo);

		return getFedoraDataUrlFromPidDatastream(id.getPid(), irUrlInfo
				.getParameters());
	}

	private String getFedoraDataUrlFromPidDatastream(String pid,
			String datastream) {
		// test:85 , DATA_FILE ->
		// http://localhost/fedora/get/test:85/DATA_FILE

		String datastreamName;

		StringBuffer url = new StringBuffer(96);

		if (datastream.contains("&")) {
			datastreamName = datastream.substring(0, datastream.indexOf('&'));
		} else {
			datastreamName = datastream;
		}

		url.append(fedoraDataUrl).append(pid).append('/')
				.append(datastreamName);

		return url.toString();
	}

	public void setBaseHostUrl(String baseHostUrl) {
		this.baseHostUrl = baseHostUrl;
	}

	public void setIdService(IdService idService) {
		this.idService = idService;
	}

	public String getFedoraDataUrl() {
		return fedoraDataUrl;
	}

	public void setFedoraDataUrl(String fedoraDataUrl) {
		this.fedoraDataUrl = fedoraDataUrl;
	}

}
