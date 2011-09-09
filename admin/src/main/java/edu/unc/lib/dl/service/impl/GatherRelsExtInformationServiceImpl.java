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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;

import edu.unc.lib.dl.schema.Id;
import edu.unc.lib.dl.schema.IrUrlInfo;
import edu.unc.lib.dl.service.DataService;
import edu.unc.lib.dl.service.GatherRelsExtInformationService;
import edu.unc.lib.dl.service.IdService;
import edu.unc.lib.dl.util.Constants;

public class GatherRelsExtInformationServiceImpl implements
	GatherRelsExtInformationService {
    private IdService idService;
    private DataService dataService;
    private final Logger logger = Logger.getLogger(getClass());
    private String baseURL;
    private String name;
    private String pass;
    private String baseIrUrl;
    private final Pattern getPid = Pattern.compile("<(info:fedora.+)>");
    private String pid;
    private IrUrlInfo irUrlInfo;

    public Map<String, String> getAllFromPid(String pid) {
	this.pid = pid;
	return gatherInformation(pid);
    }

    public Map<String, String> getAllFromIrUrlInfo(IrUrlInfo irUrlInfo) {
	Id id = idService.getId(irUrlInfo);

	return gatherInformation(id.getPid());
    }

    private Map gatherInformation(String pid) {
	Map resultsMap = new HashMap(32);
	String result = null;
	StringBuffer uidQuery = new StringBuffer();
	uidQuery.append("select $key $value from <#ri> where <info:fedora/"
		+ pid + "> $key $value");
	StringBuffer url = new StringBuffer();
	url.append(this.baseURL).append("risearch");

	// if(logger.isDebugEnabled()) logger.debug(pid);
	if (logger.isDebugEnabled()) {
	    logger.debug(uidQuery);
	}

	try {
		HttpClientParams params = new HttpClientParams();
		params.setContentCharset("UTF-8");
		HttpClient client = new HttpClient();
		client.setParams(params);
	    UsernamePasswordCredentials cred = new UsernamePasswordCredentials(
		    this.name, this.pass);
	    client.getState().setCredentials(new AuthScope(null, 443), cred);
	    client.getState().setCredentials(new AuthScope(null, 80), cred);

	    // if(logger.isDebugEnabled())
	    // logger.debug(url.toString());
	    PostMethod httppost = new PostMethod(url.toString());
	    httppost.addParameter("type", "tuples");
	    httppost.addParameter("lang", "itql");
	    httppost.addParameter("format", "CSV");
	    // logger.debug("Query: " + uidQuery);
	    httppost.addParameter("query", uidQuery.toString());
	    String literalResponse = null;
	    try {
		client.executeMethod(httppost);
		if (httppost.getStatusCode() == HttpStatus.SC_OK) {
		    BufferedReader br = new BufferedReader(
			    new InputStreamReader(httppost
				    .getResponseBodyAsStream()));
		    String readLine;
		    List<String> list = new ArrayList<String>();

		    while (((readLine = br.readLine()) != null)) {
			if (logger.isDebugEnabled()) {
			    logger.debug(readLine);
			}
			list.add(readLine);
		    }

		    processRiSearchResults(resultsMap, list, pid);

		} else {
		    logger.error("Unexpected failure: "
			    + httppost.getStatusLine().toString());
		}
	    } finally {
		httppost.releaseConnection();
	    }
	    logger.debug("Got response: " + literalResponse);

	    logger.debug("Got result: " + result);
	} catch (Exception e) {
	    logger.error("problems", e);
	}

	return resultsMap;
    }

    private void processRiSearchResults(Map map, List<String> list, String pid) {
	String riPid = Constants.RI_PID_PREFIX + pid;
	List<String> subjects = new ArrayList<String>();
	List<String> datastreams = new ArrayList<String>();
	List<String> constituents = new ArrayList<String>();
	Map<String, String> datastreamLabels = new HashMap<String, String>();

	if (list.get(0).equals(Constants.RI_RESULT_HEADER))
	    list.remove(0);

	map.put(Constants.PID, pid);

	// if(logger.isDebugEnabled()) logger.debug("Parsing ri
	// results:");
	for (int i = 0; i < list.size(); i++) {

	    String[] keyvalue = new String[2];
	    String temp = list.get(i);
	    int separator = temp.indexOf(',');
	    keyvalue[0] = temp.substring(0, separator);
	    keyvalue[1] = removeQuotes(temp.substring(separator + 1));

	    if (logger.isDebugEnabled())
		logger.debug("key: " + keyvalue[0] + " value: " + keyvalue[1]);

	    if (keyvalue[0].equals(Constants.RI_DISSEMINATOR)) {
		datastreams.add(idService.getPidFromRiPid(keyvalue[1]));
	    } else if (keyvalue[0].equals(Constants.RI_DATASTREAM_LABEL)) {
		addDatastreamLabel(datastreamLabels, keyvalue[1]);
	    } else if (keyvalue[0].equals(Constants.RI_SUBJECT)) {
		subjects.add(keyvalue[1]);
	    } else if (keyvalue[0].equals(Constants.RI_HAS_CONSTITUENT)) {
		constituents.add(idService.getPidFromRiPid(keyvalue[1]));
	    } else if (keyvalue[0].equals(Constants.RI_REPOSITORY_PATH)) {
		map.put(keyvalue[0], keyvalue[1]);
		map.put(Constants.SEARCH_URI, baseIrUrl + keyvalue[1]);
	    } else if (keyvalue[0].equals(Constants.RI_HAS_MODEL)) {
		if (!keyvalue[1].startsWith(Constants.RI_FEDORA_OBJECT)) {
		    map.put(keyvalue[0], keyvalue[1]);
		    getContentModel(map, keyvalue[1]);
		}
	    } else if (keyvalue[0].equals(Constants.RI_FIRST_CONSTITUENT)
		    || keyvalue[0].equals(Constants.RI_LAST_CONSTITUENT)
		    || keyvalue[0].equals(Constants.RI_NEXT_CONSTITUENT)
		    || keyvalue[0].equals(Constants.RI_PREV_CONSTITUENT)
		    || keyvalue[0].equals(Constants.RI_DISSEMINATOR)
		    || keyvalue[0].equals(Constants.RI_THUMBNAIL)
		    || keyvalue[0].equals(Constants.RI_TEXT)
		    || keyvalue[0].equals(Constants.RI_COLLECTION)
		    || keyvalue[0].equals(Constants.RI_IS_CONSTITUENT_OF)) {
		map.put(keyvalue[0], idService.getPidFromRiPid(keyvalue[1]));
	    } else {
		map.put(keyvalue[0], keyvalue[1]);
	    }

	}

	map.put(Constants.RI_DATASTREAM, datastreams);
	map.put(Constants.RI_SUBJECT, subjects);
	map.put(Constants.RI_HAS_CONSTITUENT, constituents);
	map.put(Constants.RI_DATASTREAM_LABEL, datastreamLabels);
    }

    private void getContentModel(Map map, String value) {
	if (value == null)
	    return;

	if (value.equals(Constants.RI_MODEL_COLLECTION)) {
	    map.put(Constants.RI_CONTENT_MODEL,
		    Constants.CONTENT_MODEL_COLLECTION);
	} else if (value.equals(Constants.RI_MODEL_FOLDER)) {
	    map.put(Constants.RI_CONTENT_MODEL, Constants.CONTENT_MODEL_FOLDER);
	} else if (value.equals(Constants.RI_MODEL_SIMPLE)) {
	    map.put(Constants.RI_CONTENT_MODEL, Constants.CONTENT_MODEL_ITEM);
	}
    }

    private void addDatastreamLabel(Map map, String value) {
	// parse "TEXT|TEI Transcript"
	if ((value == null) || (value.indexOf(Constants.PIPE) < 1)) {
	    return;
	}

	String[] temp = value.split("\\|");

	if (logger.isDebugEnabled())
	    logger.debug("datastreamLabel 0 " + temp[0]);
	if (logger.isDebugEnabled())
	    logger.debug("datastreamLabel 1 " + temp[1]);

	map.put(temp[0], temp[1]);
    }

    // Need to remove "wrapping" quotes from strings if present. Is there a
    // better way?
    private String removeQuotes(String value) {
	logger.debug("removeQuotes: " + value);

	if (value == null) {
	    return null;
	}

	if (value.startsWith(Constants.DOUBLE_QUOTE)) {
	    if (value.equals("\"\"")) {
		return Constants.EMPTY_STRING;
	    }

	    return value.substring(1, value.length() - 1);
	}

	return value;
    }

    public void setIdService(IdService idService) {
	this.idService = idService;
    }

    public String getBaseURL() {
	return baseURL;
    }

    public void setBaseURL(String baseURL) {
	this.baseURL = baseURL;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getPass() {
	return pass;
    }

    public void setPass(String pass) {
	this.pass = pass;
    }

    public void setBaseIrUrl(String baseIrUrl) {
	this.baseIrUrl = baseIrUrl;
    }

    public void setDataService(DataService dataService) {
	this.dataService = dataService;
    }
}
