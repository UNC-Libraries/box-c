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

import java.util.regex.Pattern;

import org.jdom2.Element;

import edu.unc.lib.dl.cdr.services.AbstractDatastreamEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
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
	public Enhancement<Element> getEnhancement(EnhancementMessage message) throws EnhancementException {
		return new FullTextEnhancement(this, message);
	}

	@Override
	public String getName() {
		return enhancementName;
	}
	
}
