package edu.unc.lib.dl.ui.util;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializationUtil {
	private static final Logger log = LoggerFactory.getLogger(SerializationUtil.class);

	private static ObjectMapper jsonMapper = new ObjectMapper();
	
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