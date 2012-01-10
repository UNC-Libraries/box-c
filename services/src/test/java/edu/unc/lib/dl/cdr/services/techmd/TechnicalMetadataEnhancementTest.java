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

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;

public class TechnicalMetadataEnhancementTest extends Assert {

	protected String readFileAsString(String filePath) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		java.io.InputStream inStream = this.getClass().getResourceAsStream(filePath);
		java.io.InputStreamReader inStreamReader = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(inStreamReader);
		// BufferedReader reader = new BufferedReader(new
		// InputStreamReader(this.getClass().getResourceAsStream(filePath)));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}

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
			Document result = new SAXBuilder().build(new StringReader(xmlstr));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			this.assertFalse(true);
		}
	}
}
