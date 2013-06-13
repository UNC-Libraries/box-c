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
package edu.unc.lib.dl.ui.util;

import java.io.IOException;
import java.util.Collection;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;

public class SerializationUtil {
	private static final Logger log = LoggerFactory.getLogger(SerializationUtil.class);

	private static ObjectMapper jsonMapper = new ObjectMapper();
	
	public static String resultsToJSON(SearchResultResponse resultResponse) {
		StringBuilder result = new StringBuilder();
		result.append('{');
		boolean firstEntry = true;
		for (BriefObjectMetadata metadata: resultResponse.getResultList()) {
			if (firstEntry)
				firstEntry = false;
			else result.append(',');
			result.append(metadataToJSON(metadata));
		}
		result.append('}');
		return result.toString();
	}
	
	public static String metadataToJSON(BriefObjectMetadata metadata) {
		StringBuilder result = new StringBuilder();
		result.append("'").append(metadata.getId()).append("':");
		result.append('{');
		result.append("'_version_':'").append(metadata.get_version_()).append("'");
		result.append(',');
		result.append("'status':").append(joinArray(metadata.getStatus()));
		result.append('}');
		return result.toString();
	}
	
	private static String joinArray(Collection<String> collection) {
		StringBuilder result = new StringBuilder();
		result.append('[');
		for (String value : collection) {
			if (result.length() > 1)
				result.append(',');
			result.append("'").append(value).append("'");
		}
		result.append(']');
		return result.toString();
	}
	
	public static String objectToJSON(Object object) {
		try {
			return jsonMapper.writeValueAsString(object);
		} catch (JsonGenerationException e) {
			log.error("Unable to serialize object of type " + object.getClass().getName() + " to json", e);
		} catch (JsonMappingException e) {
			log.error("Unable to serialize object of type " + object.getClass().getName() + " to json", e);
		} catch (IOException e) {
			log.error("Unable to serialize object of type " + object.getClass().getName() + " to json", e);
		}
		return "";
	}
}