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
package edu.unc.lib.dl.search;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.List;

public class SearchProcessor implements Serializable {
    private List<SearchResult> searchResults;

    public void sendQuery(String query) {

	// Reader data, Writer output
	// HttpURLConnection httpUrlConn = null;
	// try {
	// httpUrlConn = (HttpURLConnection) solrUrl.openConnection();
	// try {
	// httpUrlConn.setRequestMethod("POST");
	// } catch (ProtocolException e) {
	// throw new PostException("Shouldn't happen: HttpURLConnection doesn't
	// support POST??", e);
	// }
	// httpUrlConn.setDoOutput(true);
	// httpUrlConn.setDoInput(true);
	// httpUrlConn.setUseCaches(false);
	// httpUrlConn.setAllowUserInteraction(false);
	// httpUrlConn.setRequestProperty("Content-type", "text/xml; charset=" +
	// POST_ENCODING);
	//        
	// OutputStream out = httpUrlConn.getOutputStream();
	//        
	// try {
	// Writer writer = new OutputStreamWriter(out, POST_ENCODING);
	// bufferedRead(data, writer);
	// writer.close();
	// } catch (IOException e) {
	// throw new PostException("IOException while posting data", e);
	// } finally {
	// if(out!=null) out.close();
	// }
	//        
	// InputStream in = httpUrlConn.getInputStream();
	// try {
	// Reader reader = new InputStreamReader(in);
	// bufferedRead(reader, output);
	// reader.close();
	// } catch (IOException e) {
	// throw new PostException("IOException while reading response", e);
	// } finally {
	// if(in!=null) in.close();
	// }
	//        
	// } catch (IOException e) {
	// try {
	// fatal("Solr returned an error: " + httpUrlConn.getResponseMessage());
	// } catch (IOException f) { }
	// fatal("Connection error (is Solr running at " + solrUrl + " ?): " +
	// e);
	// } finally {
	// if(httpUrlConn!=null) httpUrlConn.disconnect();
	// }
    }

    private static void bufferedRead(Reader reader, Writer writer)
	    throws IOException {
	char[] buffer = new char[1024];
	int count = 0;
	while ((count = reader.read(buffer)) >= 0) {
	    writer.write(buffer, 0, count);
	}
	writer.flush();
    }

    public List<SearchResult> getSearchResults() {
	return searchResults;
    }

    public void setSearchResults(List<SearchResult> searchResults) {
	this.searchResults = searchResults;
    }
}
