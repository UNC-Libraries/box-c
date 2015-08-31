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
package edu.unc.lib.dl.cdr.services.imaging;

import java.util.regex.Pattern;

import org.jdom2.Element;

import edu.unc.lib.dl.cdr.services.AbstractDatastreamEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

/**
 * Enhancement service used for construction of jp2 derived images.
 * 
 * @author Gregory Jansen
 * @author bbpennel
 */
public class ImageEnhancementService extends AbstractDatastreamEnhancementService {
	public static final String enhancementName = "Image Derivative Generation";

	public ImageEnhancementService() {
		super();
	}

	public void init() {
		mimetypePattern = Pattern.compile("^image/.*");
		derivativeDatastream = Datastream.IMAGE_JP2000.getName();
	}

	@Override
	public Enhancement<Element> getEnhancement(EnhancementMessage message) {
		return new ImageEnhancement(this, message);
	}

	@Override
	public String getName() {
		return enhancementName;
	}
}
