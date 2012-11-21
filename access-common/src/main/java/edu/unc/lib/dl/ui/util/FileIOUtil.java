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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.HttpMethodBase;

public class FileIOUtil {

	public static void stream(OutputStream outStream, HttpMethodBase method) throws IOException{
		InputStream in = method.getResponseBodyAsStream();
		BufferedInputStream reader = null;
		try {
			reader = new BufferedInputStream(in);
			byte[] buffer = new byte[4096];
			int count = 0;
			int length = 0;
			while ((length = reader.read(buffer)) >= 0) {
				outStream.write(buffer, 0, length);
				if (count++ % 5 == 0){
					outStream.flush();
				}
			}
			outStream.flush();
		} finally {
			if (reader != null)
				reader.close();
			if (in != null)
				in.close();
		}
	}
}
