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
package edu.unc.lib.dl.cdr.services.text;

import java.util.List;
import java.util.regex.Pattern;

import org.jdom2.Element;

import edu.unc.lib.dl.cdr.services.AbstractDatastreamEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

/**
 * Service which extracts and stores the full text for supported file types.
 * 
 * @author bbpennel
 *
 */
public class FullTextEnhancementService extends AbstractDatastreamEnhancementService {
	public static final String enhancementName = "Full Text Extraction";

	public void init() {
		mimetypePattern = Pattern.compile("^(text/|application/pdf|application/msword|application/vnd\\.|application/rtf|application/powerpoint|application/postscript).*");
		derivativeDatastream = Datastream.MD_FULL_TEXT.getName();
		
	}
	
	@Override
	protected boolean isDatastreamApplicable(PID pid) throws FedoraException {
		edu.unc.lib.dl.fedora.types.Datastream dataDoc
				= managementClient.getDatastream(pid, Datastream.DATA_FILE.getName());
		
		// Don't process if there is no original data
		if (dataDoc == null) {
			return false;
		}
		
		// Filter out objects with non-applicable mimetypes
		if (!isMimetypeApplicable(pid, dataDoc)) {
			return false;
		}
		
		edu.unc.lib.dl.fedora.types.Datastream derivDoc
				= managementClient.getDatastream(pid, derivativeDatastream);
		
		// No derivative present
		if (derivDoc == null) {
			// Check for placeholder indicating that no full text datastream is needed since the extract was empty
			List<String> result =
					tripleStoreQueryService.fetchBySubjectAndPredicate(pid, CDRProperty.fullText.toString());
			return result == null || result.size() == 0 || Boolean.parseBoolean(result.get(0));
		}
		
		// Derivative is older than the original data, need to reperform the enhancement
		// Dates are in iso8601/UTC format, so lexographic string comparison is sufficient
		return dataDoc.getCreateDate().compareTo(derivDoc.getCreateDate()) > 0;
	}
	
	@Override
	public Enhancement<Element> getEnhancement(EnhancementMessage message) throws EnhancementException {
		return new FullTextEnhancement(this, message);
	}

	@Override
	public String getName() {
		return enhancementName;
	}
	
}
