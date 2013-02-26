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
package fedorax.server.module.storage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fcrepo.server.errors.authorization.AuthzDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class resolves URIs to staged file locations accessible to the Fedora
 * server. In addition it performs path security checks on any file protocol
 * URLs. (The absolute path to a resolved file must match a file staging
 * location.)
 * 
 * @author Gregory Jansen
 * 
 */
public class StagingManager implements StagingManagerMBean {

	private static final Logger LOG = LoggerFactory
			.getLogger(StagingManager.class);

	private Map<Pattern, String> urlPatternMap = new HashMap<Pattern, String>();
	private Set<String> safeCanonicalFilePaths = new HashSet<String>();

	public StagingManager() {
	}

	/**
	 * @param url
	 * @return
	 */
	public String resolveStageLocation(String url) throws AuthzDeniedException {
		if(url == null || url.trim().length() == 0) {
			return url;
		}
		for(Pattern p : this.urlPatternMap.keySet()) {
			Matcher matcher = p.matcher(url);
			if(matcher.matches()) {
				url = matcher.replaceFirst(this.urlPatternMap.get(p));
				if(url.startsWith("file:")) {
					if(!isSafeFilePath(url)) {
						throw new AuthzDeniedException(
								"Canonical staged path is not within a safe staging area: "
										+ url);
					}
				}
			}
		}
		return url;
	}

	private boolean isSafeFilePath(String url) {
		try {
			URL fileUrl = new URL(url);
			File f = new File(fileUrl.toURI()).getCanonicalFile();
			try {
				f = f.getCanonicalFile();
			} catch (IOException e) {
				return false;
			}
			String fileURI = f.toURI().toString();
			for (String safeCanonicalPath : this.safeCanonicalFilePaths) {
				if (fileURI.startsWith(safeCanonicalPath)) {
					return true;
				}
			}
		} catch (Exception e) {
		}
		return false;
	}

	public Map<String, String> getURLPatternMap() {
		Map<String, String> result = new HashMap<String, String>();
		for(Entry<Pattern, String> e : this.urlPatternMap.entrySet()) {
			result.put(e.getKey().pattern(), e.getValue());
		}
		return result;
	}

	public void setReplacementUrlPatterns(Map<String, String> stageToFileMap) {
		this.urlPatternMap.clear();
		this.safeCanonicalFilePaths.clear();
		for(Entry<String, String> e: stageToFileMap.entrySet()) {
			this.addURLPattern(e.getKey(), e.getValue());
		}
		dumpPatternsToLog();
	}

	public void addURLPattern(String matchRegex, String replacement) {
		LOG.info("Adding staging location regex: " + matchRegex + " => "
				+ replacement);
		this.urlPatternMap.put(Pattern.compile(matchRegex), replacement);
		if(replacement.startsWith("file:")) {
			// replacement strings using file: protocol must start with safe canonical file paths
			// e.g. file:/mnt/sfc/$1 starts with file:/mnt/sfc/
			//      so any staged locations that are canonical under there are safe
			this.safeCanonicalFilePaths.add(replacement.substring(0, replacement.indexOf('$')));
		}
	}

	public void removeURLPattern(String regex) {
		for(Pattern p : this.urlPatternMap.keySet()) {
			if(regex.equals(p.pattern())) {
				this.urlPatternMap.remove(p);
			}
		}
		dumpPatternsToLog();
	}
	
	public void clearURLPatterns() {
		this.urlPatternMap.clear();
		this.safeCanonicalFilePaths.clear();
	}

	public void dumpPatternsToLog() {
		StringBuilder prn = new StringBuilder();
		prn.append("StagingManager mappings:\n");
		for (Entry<Pattern, String> e : this.urlPatternMap.entrySet()) {
			prn.append('\t').append(e.getKey()).append(" => ").append(e.getValue()).append('\n');
		}
		LOG.info(prn.toString());
	}

}
