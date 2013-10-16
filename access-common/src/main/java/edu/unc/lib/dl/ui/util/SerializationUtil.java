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
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.Tag;
import edu.unc.lib.dl.util.DateTimeUtil;

public class SerializationUtil {
	private static final Logger log = LoggerFactory.getLogger(SerializationUtil.class);

	private static ObjectMapper jsonMapper = new ObjectMapper();
	{
		jsonMapper.setSerializationInclusion(Inclusion.NON_EMPTY);
		//jsonMapper.getSerializerProvider().
	}
	
	
	
	public static String structureToJSON(HierarchicalBrowseResultResponse response, AccessGroupSet groups) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("root", response.getRootNode());
		return objectToJSON(result);
	}
	
	public static String resultsToJSON(SearchResultResponse resultResponse, AccessGroupSet groups) {
		StringBuilder result = new StringBuilder();
		result.append('[');
		boolean firstEntry = true;
		for (BriefObjectMetadata metadata: resultResponse.getResultList()) {
			if (firstEntry)
				firstEntry = false;
			else result.append(',');
			result.append(metadataToJSON(metadata, groups));
		}
		result.append(']');
		return result.toString();
	}
	
	public static Map<String, Object> metadataToMap(BriefObjectMetadata metadata, AccessGroupSet groups) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("id", metadata.getId());
		
		if (metadata.getTitle() != null) 
			result.put("title", metadata.getTitle());
		
		if (metadata.get_version_() != null)
			result.put("_version_", metadata.get_version_());
		
		if (metadata.getStatus() != null && metadata.getStatus().size() > 0)
			result.put("status", metadata.getStatus());
		
		if (metadata.getSubject() != null)
			result.put("subject", metadata.getSubject());
		
		if (metadata.getResourceType() != null)
			result.put("type", metadata.getResourceType());
		
		if (metadata.getContentModel() != null && metadata.getContentModel().size() > 0)
			result.put("model", metadata.getContentModel());
		
		if (metadata.getCreator() != null)
			result.put("creator", metadata.getCreator());
		
		if (metadata.getDatastream() != null)
			result.put("datastream", metadata.getDatastream());
		
		if (metadata.getIdentifier() != null)
			result.put("identifier", metadata.getIdentifier());
		
		if (metadata.getTags() != null)
			result.put("tags", metadata.getTags());
		
		if (metadata.getCountMap() != null && metadata.getCountMap().size() > 0)
			result.put("counts", metadata.getCountMap());
		
		try {
			if (metadata.getDateAdded() != null) {
				String dateAdded = DateTimeUtil.formatDateToUTC(metadata.getDateAdded());
				result.put("added", dateAdded);
			}
			if (metadata.getDateUpdated() != null) {
				String dateUpdated = DateTimeUtil.formatDateToUTC(metadata.getDateUpdated());
				result.put("updated", dateUpdated);
			}
		} catch (ParseException e) {
			log.debug("Failed to parse date field for " + metadata.getId(), e);
		}
		
		if (metadata.getDateCreated() != null)
			result.put("created", metadata.getDateCreated());
		
		if (groups != null && metadata.getAccessControlBean() != null)
			result.put("permissions", metadata.getAccessControlBean().getPermissionsByGroups(groups));
		
		return result;
	}
	
	public static String metadataToJSON(BriefObjectMetadata metadata, AccessGroupSet groups) {
		try {
			return jsonMapper.writeValueAsString(metadataToMap(metadata, groups));
		} catch (JsonGenerationException e) {
			log.error("Unable to serialize object " + metadata.getId() + " to json", e);
		} catch (JsonMappingException e) {
			log.error("Unable to serialize object " + metadata.getId() + " to json", e);
		} catch (IOException e) {
			log.error("Unable to serialize object " + metadata.getId() + " to json", e);
		}
		return null;
	}
	
	private static String joinArray(Collection<String> collection) {
		StringBuilder result = new StringBuilder();
		result.append('[');
		for (String value : collection) {
			if (result.length() > 1)
				result.append(',');
			result.append('"').append(value.replace("\"", "\\\"").replace("\n", "\\n")).append('"');
		}
		result.append(']');
		return result.toString();
	}
	
	private static String joinTags(Collection<Tag> collection) {
		StringBuilder result = new StringBuilder();
		result.append('[');
		for (Tag value : collection) {
			if (result.length() > 1)
				result.append(',');
			result.append("{\"label\":\"").append(value.getLabel()).append('"');
			result.append(",\"text\":\"").append(value.getText()).append('"').append('}');
		}
		result.append(']');
		return result.toString();
	}
	
	private static String joinMap(Map<?, ?> map) {
		StringBuilder result = new StringBuilder();
		result.append('{');
		for (Entry<?, ?> entry : map.entrySet()) {
			if (result.length() > 1)
				result.append(',');
			result.append('"').append(entry.getKey().toString()).append('"').append(':');
			if (entry.getValue() instanceof Number)
				result.append(entry.getValue().toString());
			else result.append('"').append(entry.getValue().toString()).append('"');
		}
		result.append('}');
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