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

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;

public class SerializationUtilTest extends Assert {

	@Test
	public void briefMetadataToJSONTest() {
		BriefObjectMetadataBean md = new BriefObjectMetadataBean();
		md.setId("uuid:test");
		md.setTitle("Test Item");
		md.setIsPart(Boolean.FALSE);
		md.setDatastream(Arrays.asList("DATA_FILE|image/jpeg|orig|582753|]"));
	}

	@Test
	public void briefMetadataListToJSONTest() {
		BriefObjectMetadataBean md = new BriefObjectMetadataBean();
		md.setId("uuid:test");
		md.setTitle("Test Item");
		md.setIsPart(Boolean.FALSE);
		md.setDatastream(Arrays.asList("DATA_FILE|image/jpeg|orig|582753|]"));

		BriefObjectMetadataBean md2 = new BriefObjectMetadataBean();
		md2.setId("uuid:test2");
		md2.setTitle("Test Item 2");
		md.setIsPart(Boolean.FALSE);
		md2.setDatastream(Arrays.asList("DATA_FILE|application/msword|orig|596318|",
				"NORM_FILE|application/pdf|deriv|290733|", "MD_TECHNICAL|text/xml|admin|6406|",
				"AUDIT|text/xml|admin|405|", "RELS-EXT|text/xml|admin|1042|", "MD_DESCRIPTIVE|text/xml|meta|2301|",
				"DC|text/xml|meta|809|", "MD_EVENTS|text/xml|meta|5063|"));

		List<BriefObjectMetadataBean> mdList = Arrays.asList(md, md2);

		assertTrue(SerializationUtil.objectToJSON(mdList).length() > 2);
	}
}
