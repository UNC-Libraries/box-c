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

import java.util.List;
import java.util.regex.Pattern;

import org.jdom2.Element;

import edu.unc.lib.dl.cdr.services.AbstractDatastreamEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.JMSMessageUtil;

/**
 * Generates surrogate thumbnail images of items with data_files of image format types.
 * 
 * @author Gregory Jansen
 * @author bbpennel
 * 
 */
public class ThumbnailEnhancementService extends AbstractDatastreamEnhancementService {
	public static final String enhancementName = "Thumbnail Generation";

	public ThumbnailEnhancementService() {
		super();
	}

	public void init() {
		mimetypePattern = Pattern.compile("^image/.*");
		derivativeDatastream = Datastream.THUMB_LARGE.getName();
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
			return true;
		}
		
		// Derivative is older than the original data, need to reperform the enhancement
		if (dataDoc.getCreateDate().compareTo(derivDoc.getCreateDate()) > 0) {
			return true;
		}
		
		// If there are any objects using this object as a surrogate, check to see if their derivatives are out of date
		List<PID> surrogateHolders = tripleStoreQueryService
				.fetchByPredicateAndLiteral(CDRProperty.hasSurrogate.toString(), pid);
		if (surrogateHolders != null) {
			for (PID surrogateHolder : surrogateHolders) {
				edu.unc.lib.dl.fedora.types.Datastream surrDoc
						= managementClient.getDatastream(surrogateHolder, derivativeDatastream);
				
				if (derivDoc == null || dataDoc.getCreateDate().compareTo(derivDoc.getCreateDate()) > 0) {
					return true;
				}
			}
		}
		
		return false;
	}

	@Override
	public Enhancement<Element> getEnhancement(EnhancementMessage message) {
		return new ThumbnailEnhancement(this, message);
	}

	@Override
	public boolean prefilterMessage(EnhancementMessage message) throws EnhancementException {
		String action = message.getQualifiedAction();

		if (JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.equals(action))
			return true;
		if (JMSMessageUtil.ServicesActions.APPLY_SERVICE.equals(action))
			return this.getClass().getName().equals(message.getServiceName());

		// If its not a Fedora message at this point, then its not going to match anything else
		if (!(message instanceof FedoraEventMessage))
			return false;

		if (JMSMessageUtil.FedoraActions.INGEST.equals(action))
			return true;

		if (JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_REFERENCE.equals(action)
				|| JMSMessageUtil.FedoraActions.ADD_DATASTREAM.equals(action)
				|| JMSMessageUtil.FedoraActions.MODIFY_DATASTREAM_BY_VALUE.equals(action)) {
			String datastream = ((FedoraEventMessage) message).getDatastream();
			return ContentModelHelper.Datastream.DATA_FILE.equals(datastream);
		}

		if (!(JMSMessageUtil.FedoraActions.ADD_RELATIONSHIP.equals(action) || JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP
				.equals(action))) {
			return false;
		}

		String relationship = ((FedoraEventMessage) message).getRelationPredicate();
		return ContentModelHelper.CDRProperty.sourceData.equals(relationship)
				|| ContentModelHelper.CDRProperty.hasSurrogate.equals(relationship);
	}

	@Override
	public String getName() {
		return enhancementName;
	}
}
