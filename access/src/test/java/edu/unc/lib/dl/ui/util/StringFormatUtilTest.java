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
}
