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
package edu.unc.lib.dl.security;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.fcrepo.server.security.xacml.pdp.finder.AttributeFinderException;
import org.fcrepo.server.security.xacml.pdp.finder.attribute.DesignatorAttributeFinderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.StandardAttributeFactory;
import com.sun.xacml.cond.EvaluationResult;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;

public class CdrRIAttributeFinder extends DesignatorAttributeFinderModule {

	private static final Logger logger = LoggerFactory
			.getLogger(CdrRIAttributeFinder.class);

	private static Set<String> myAttributes = new HashSet<String>();

	static {
		myAttributes.add(ContentModelHelper.UserRole.curator.getURI()
				.toString());
		myAttributes.add(ContentModelHelper.CDRProperty.embargo.getURI()
				.toString());
		myAttributes.add(ContentModelHelper.UserRole.ingester.getURI()
				.toString());
		myAttributes.add(ContentModelHelper.UserRole.observer.getURI()
				.toString());
		myAttributes.add(ContentModelHelper.UserRole.patron.getURI()
				.toString());
		myAttributes.add(ContentModelHelper.UserRole.metadataPatron.getURI()
				.toString());
		myAttributes.add(ContentModelHelper.UserRole.accessCopiesPatron.getURI()
				.toString());
		myAttributes.add(ContentModelHelper.UserRole.processor.getURI()
				.toString());
		myAttributes.add(ContentModelHelper.CDRProperty.embargo.getURI()
				.toString());
		myAttributes.add(ContentModelHelper.CDRProperty.dataAccessCategory.getURI()
				.toString());
	}

	private AttributeFactory attributeFactory = null;

	private AccessControlUtils accessControlUtils;

	public CdrRIAttributeFinder() {
		super();
		attributeFactory = StandardAttributeFactory.getFactory();
		logger.info("Initialised AttributeFinder:" + this.getClass().getName());
		if (logger.isDebugEnabled()) {
			logger.debug("registering the following attributes: ");
			for (Integer desNum : this.getSupportedDesignatorTypes()) {
				logger.debug("Designator Type: " + desNum);
				for (String attrName : m_attributes.get(desNum).keySet()) {
					logger.debug("\t" + attrName);
				}
			}
		}
	}

	/**
	 * Used to get an attribute. If one of those values isn't being asked for,
	 * or if the types are wrong, then an empty bag is returned.
	 * 
	 * @param attributeType
	 *            the datatype of the attributes to find, which must be time,
	 *            date, or dateTime for this module to resolve a value
	 * @param attributeId
	 *            the identifier of the attributes to find, which must be one of
	 *            the three ENVIRONMENT_* fields for this module to resolve a
	 *            value
	 * @param issuer
	 *            the issuer of the attributes, or null if unspecified
	 * @param subjectCategory
	 *            the category of the attribute or null, which ignored since
	 *            this only handles non-subjects
	 * @param context
	 *            the representation of the request data
	 * @param designatorType
	 *            the type of designator
	 * @return the result of attribute retrieval, which will be a bag with a
	 *         single attribute, an empty bag, or an error
	 */
	@Override
	public EvaluationResult findAttribute(URI attributeType, URI attributeId,
			URI issuer, URI subjectCategory, EvaluationCtx context,
			int designatorType) {

		long startTime = System.currentTimeMillis();

		String resourceId = context.getResourceId().encode();
		if (logger.isDebugEnabled()) {
			logger.debug("CdrRIAttributeFinder: [" + attributeType.toString()
					+ "] " + attributeId + ", rid=" + resourceId);
		}

		if (resourceId == null || resourceId.equals("")) {
			return new EvaluationResult(
					BagAttribute.createEmptyBag(attributeType));
		}

		if (resourceId.equals("/FedoraRepository")) {
			return new EvaluationResult(
					BagAttribute.createEmptyBag(attributeType));
		}

		// figure out which attribute we're looking for
		String attrName = attributeId.toString();

		// we only know about registered attributes from config file
		if (!getSupportedDesignatorTypes()
				.contains(new Integer(designatorType))) {
			if (logger.isDebugEnabled()) {
				logger.debug("Does not know about designatorType: "
						+ designatorType);
			}
			return new EvaluationResult(
					BagAttribute.createEmptyBag(attributeType));
		}

		Set<String> allowedAttributes = m_attributes.get(designatorType)
				.keySet();
		if (!allowedAttributes.contains(attrName)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Does not know about attribute: " + attrName);
			}
			return new EvaluationResult(
					BagAttribute.createEmptyBag(attributeType));
		}

		EvaluationResult result = null;
		try {
			result = getEvaluationResult(resourceId, attrName, designatorType,
					attributeType);
		} catch (Exception e) {
			logger.error("Error finding attribute: " + e.getMessage(), e);
			return new EvaluationResult(
					BagAttribute.createEmptyBag(attributeType));
		}

		logger.info("Total time for CDR role lookup: "
				+ (System.currentTimeMillis() - startTime) + " milliseconds");
		return result;
	}

	/**
	 * 
	 * @param resourceID
	 *            - the hierarchical XACML resource ID
	 * @param attribute
	 *            - attribute to get - this is a URI that maps to a Fedora
	 *            relationship name
	 * @param type
	 * @return
	 * @throws AttributeFinderException
	 */
	private EvaluationResult getEvaluationResult(String resourceID,
			String attribute, int designatorType, URI type)
			throws AttributeFinderException {

		// split up the path of the hierarchical resource id
		// either the last part is the pid, or the last-but one is the pid and
		// the last is the datastream
		String resourceParts[] = resourceID.split("/");
		String subject; // the full subject, ie pid or pid/ds
		String pid;
		if (resourceParts.length > 1) {
			if (resourceParts[resourceParts.length - 1].contains(":")) {
				// ends with a pid, we have pid only
				subject = resourceParts[resourceParts.length - 1];
				pid = subject;
			} else { // datastream
				pid = resourceParts[resourceParts.length - 2];
				subject = pid + "/" + resourceParts[resourceParts.length - 1];
			}
		} else {
			// eg /FedoraRepository, not a valid path to PID or PID/DS
			logger.debug("Resource ID not valid path to PID or datastream: "
					+ resourceID);
			return new EvaluationResult(BagAttribute.createEmptyBag(type));
		}

		logger.debug("Getting attribute " + attribute + " for resource " + pid);

		// also compute list permission
		Set<String> groups = accessControlUtils.getGroupsInRole(new PID(pid), attribute);

		if (groups == null || groups.isEmpty()) {
			return new EvaluationResult(BagAttribute.createEmptyBag(type));
		}

		Set<AttributeValue> bagValues = new HashSet<AttributeValue>();
		logger.debug("Attribute values found: " + groups.size());
		for (String s : groups) {
			AttributeValue attributeValue = null;
			try {
				attributeValue = attributeFactory.createValue(type, s);
			} catch (Exception e) {
				logger.error("Error creating attribute: " + e.getMessage(), e);
				continue;
			}

			bagValues.add(attributeValue);

			if (logger.isDebugEnabled()) {
				logger.debug("AttributeValue found: [" + type.toASCIIString()
						+ "] " + s);
			}

		}
		BagAttribute bag = new BagAttribute(type, bagValues);
		return new EvaluationResult(bag);
	}
}