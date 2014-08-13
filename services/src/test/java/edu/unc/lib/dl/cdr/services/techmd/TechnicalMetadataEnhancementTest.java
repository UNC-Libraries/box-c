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

package edu.unc.lib.dl.cdr.services.techmd;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;

public class TechnicalMetadataEnhancementTest extends Assert {

	@Test
	public void testFITSResponseParsing(){
		try {
			java.io.InputStream inStream = this.getClass().getResourceAsStream("fitsOutputMultipleLineBreaks.xml");
			java.io.BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));

			StringBuilder xml = new StringBuilder();
			StringBuilder err = new StringBuilder();
			boolean blankReached = false;
			String previousLine = "";
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (line.trim().length() == 0 && previousLine.contains("WARNING:")) {
					blankReached = true;
					continue;
				} else {

					if (blankReached) {
						err.append(line).append("\n");
					} else {
						if (line.indexOf("\n") == -1)
							xml.append(line).append("\n");
					}
				}
			}

			String xmlstr = xml.toString();
			System.out.println("test" + xmlstr);
			new SAXBuilder().build(new StringReader(xmlstr));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertFalse(true);
		}
	}
}
