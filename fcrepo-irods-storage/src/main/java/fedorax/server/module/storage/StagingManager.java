/**
 * Copyright 2011 The University of North Carolina at Chapel Hill
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
package fedorax.server.module.storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

/**
 * @author Gregory Jansen
 *
 */
public class StagingManager implements StagingManagerMBean {
	private static final Logger LOG = Logger.getLogger(StagingManager.class);
	private static StagingManager _instance = null;

	private Map<String, String> stageToFileMap = new HashMap<String, String>();

	public static StagingManager instance() {
		if(_instance == null) {
			_instance = new StagingManager();
		}
		return _instance;
	}

	private StagingManager() {
		// load mappings if any on disk
	}

	/**
	 * @param url
	 * @return
	 */
	public boolean isStagedLocation(String url) {
		if(url == null || url.trim().length() == 0) {
			return false;
		}
		for(String stage : this.stageToFileMap.keySet()) {
			if(url.startsWith(stage)) return true;
		}
		return false;
	}

	/**
	 * @param url
	 * @return
	 */
	public String rewriteStagedLocation(String url) {
		if(url == null || url.trim().length() == 0) {
			return url;
		}
		for(String stage : this.stageToFileMap.keySet()) {
			if(url.startsWith(stage)) {
				// rewrite
				return url.replaceFirst(stage, this.stageToFileMap.get(stage));
			}
		}
		return url;
	}

	public boolean isFileInStagedLocation(File f) {
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			return false;
		}
		String fileURI = f.toURI().toString();
		for(String stageURL : this.stageToFileMap.values()) {
			if(fileURI.startsWith(stageURL)) {
				return true;
			}
		}
		return false;
	}

	public Map<String, String> getStageToFileMap() {
		return stageToFileMap;
	}

	public void setStageToFileMap(Map<String, String> stageToFileMap) {
		this.stageToFileMap = stageToFileMap;
		dumpMappingsToLog();
	}

	public void addStage(String stageBase, String fileBase) {
		this.stageToFileMap.put(stageBase, fileBase);
		dumpMappingsToLog();
	}

	public void removeStage(String stageBase) {
		this.stageToFileMap.remove(stageBase);
		dumpMappingsToLog();
	}

	private void dumpMappingsToLog() {
		StringBuilder prn = new StringBuilder();
		prn.append("StagingManager mappings:\n");
		for(Entry<String, String> e : this.stageToFileMap.entrySet()) {
			prn.append(e.getKey()).append(" => ").append(e.getValue());
		}
		LOG.info(prn.toString());
	}

}
