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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;

public class StringFormatUtilTest extends Assert {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(StringFormatUtilTest.class);
	
	@Test
	public void truncateText() throws IOException{
		String abstractText = IOUtils.toString(this.getClass().getResourceAsStream("multilineAbstract.txt"), "UTF-8");
		
		String truncated = StringFormatUtil.truncateText(abstractText, 100);
		assertEquals(truncated.length(), 99);
		
		abstractText = "t" + abstractText;
		truncated = StringFormatUtil.truncateText(abstractText, 100);
		assertEquals(truncated.length(), 100);
		
		try {
			truncated = StringFormatUtil.truncateText(abstractText, -1);
			fail();
		} catch (IndexOutOfBoundsException e){
		}
		
		truncated = StringFormatUtil.truncateText(abstractText, abstractText.length() + 10);
		assertEquals(truncated.length(), abstractText.length());
		
		truncated = StringFormatUtil.truncateText(null, 100);
		assertNull(truncated);
	}
	
	@Test
	public void regexTest() {
		Pattern oldFacetPath = Pattern.compile("(setFacet:)?path[,:]\"?\\d+,(uuid:[a-f0-9\\-]+)(!\\d+)?");
		String facet = "setFacet:path,\"2,uuid:9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d9!3\"|resetNav:search";
		if (facet != null) {
			Matcher matches = oldFacetPath.matcher(facet);
			if (matches.find()) {
				String pid = matches.group(2);
				boolean isList = matches.group(3) != null;
			}
		}
	}

	@Test
	public void makeTokenTest() {
		assertEquals("blah", StringFormatUtil.makeToken("blah", "_"));
		assertEquals("caf_", StringFormatUtil.makeToken("café", "_"));
		assertEquals("lorem_ipsum_", StringFormatUtil.makeToken("lorem ipsum?", "_"));
		assertEquals("lorem__ipsum_", StringFormatUtil.makeToken("lorem? ipsum?", "_"));
		assertEquals("lorem___ipsum", StringFormatUtil.makeToken("lorem;  ipsum", "_"));
	}
	
}
