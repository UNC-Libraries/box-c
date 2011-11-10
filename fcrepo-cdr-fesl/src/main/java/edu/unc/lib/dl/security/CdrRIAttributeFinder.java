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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.StandardAttributeFactory;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.finder.AttributeFinderModule;

import edu.unc.lib.dl.fedora.AccessControlUtils;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.util.TripleStoreQueryServiceMulgaraImpl;
import edu.unc.lib.dl.util.TripleStoreQueryService.PathInfo;

import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.pdp.finder.AttributeFinderConfigUtil;
import org.fcrepo.server.security.xacml.pdp.finder.AttributeFinderException;
import org.fcrepo.server.security.xacml.util.AttributeFinderConfig;
import org.fcrepo.server.security.xacml.util.ContextUtil;
import org.fcrepo.server.security.xacml.util.RelationshipResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdrRIAttributeFinder extends AttributeFinderModule {

	private static final Logger logger = LoggerFactory
			.getLogger(CdrRIAttributeFinder.class);

	private AttributeFactory attributeFactory = null;

	private RelationshipResolver relationshipResolver = null;

	// private Map<Integer, Set<String>> attributes = null;
	private AttributeFinderConfig attributes = null;

	// Attributes used by the CDR
	private String[] specialAttributes = {
			"http://cdr.unc.edu/definitions/1.0/base-model.xml#permitMetadataRead",
			"http://cdr.unc.edu/definitions/1.0/base-model.xml#permitOriginalsRead",
			"http://cdr.unc.edu/definitions/1.0/base-model.xml#permitDerivativesRead",
			"http://cdr.unc.edu/definitions/1.0/base-model.xml#permitMetadataCreate",
			"http://cdr.unc.edu/definitions/1.0/base-model.xml#permitMetadataUpdate",
			"http://cdr.unc.edu/definitions/1.0/base-model.xml#permitMetadataDelete",
			"http://cdr.unc.edu/definitions/1.0/base-model.xml#permitOriginalsCreate",
			"http://cdr.unc.edu/definitions/1.0/base-model.xml#permitOriginalsUpdate",
			"http://cdr.unc.edu/definitions/1.0/base-model.xml#permitOriginalsDelete",
			"http://cdr.unc.edu/definitions/1.0/base-model.xml#permitDerivativesCreate",
			"http://cdr.unc.edu/definitions/1.0/base-model.xml#permitDerivativesUpdate",
			"http://cdr.unc.edu/definitions/1.0/base-model.xml#permitDerivativesDelete" };

	private AccessControlUtils accessControlUtils;

	public CdrRIAttributeFinder() {
		try {
			attributes = AttributeFinderConfigUtil
					.getAttributeFinderConfig(this.getClass().getName());
			logger.info("Initialised AttributeFinder:"
					+ this.getClass().getName());

			if (logger.isDebugEnabled()) {
				logger.debug("registering the following attributes: ");
				for (int desNum : attributes.getDesignatorIds()) {
					for (String attrName : attributes.get(desNum)
							.getAttributeNames()) {
						logger.debug(desNum + ": " + attrName);
					}
				}
			}

			Map<String, String> resolverConfig = AttributeFinderConfigUtil
					.getResolverConfig(this.getClass().getName());
			if (logger.isDebugEnabled()) {
				for (String s : resolverConfig.keySet()) {
					logger.debug(s + ": " + resolverConfig.get(s));
				}
			}

			relationshipResolver = ContextUtil.getInstance()
					.getRelationshipResolver();

			attributeFactory = StandardAttributeFactory.getFactory();

			Map<String, String> options = AttributeFinderConfigUtil
					.getOptionMap(this.getClass().getName());

			// Load CDR configuration
			accessControlUtils = new AccessControlUtils();
			accessControlUtils.loadSettingsFromOptions(options);
			accessControlUtils.initForFedoraBasedAccessControl(false,
					attributeFactory);
			accessControlUtils
					.startCacheCleanupThreadForFedoraBasedAccessControl();

		} catch (AttributeFinderException afe) {
			logger.error("Attribute finder not initialised:"
					+ this.getClass().getName(), afe);
		}
	}

	/**
	 * Returns true always because this module supports designators.
	 * 
	 * @return true always
	 */
	@Override
	public boolean isDesignatorSupported() {
		return true;
	}

	/**
	 * Returns a <code>Set</code> with a single <code>Integer</code> specifying
	 * that environment attributes are supported by this module.
	 * 
	 * @return a <code>Set</code> with
	 *         <code>AttributeDesignator.ENVIRONMENT_TARGET</code> included
	 */
	@Override
	public Set<Integer> getSupportedDesignatorTypes() {
		return attributes.getDesignatorIds();
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
	 *            the type of designator, which must be ENVIRONMENT_TARGET for
	 *            this module to resolve a value
	 * @return the result of attribute retrieval, which will be a bag with a
	 *         single attribute, an empty bag, or an error
	 */
	@Override
	public EvaluationResult findAttribute(URI attributeType, URI attributeId,
			URI issuer, URI subjectCategory, EvaluationCtx context,
			int designatorType) {

		long totalTime = System.currentTimeMillis();

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

		// CDR processing; skip other processing if found
		for (int i = 0; i < specialAttributes.length; i++) {
			if (specialAttributes[i].equals(attrName)) {

				String temp;

				if (resourceId.startsWith("/")) { // maybe this is always the
					// case and we should just
					// get the substring
					temp = resourceId.substring(1);
				} else {
					temp = resourceId;
				}

				return accessControlUtils.processCdrAccessControl(temp,
						attrName, attributeType);
			}
		}
		// we only know about registered attributes from config file
		if (!attributes.getDesignatorIds()
				.contains(new Integer(designatorType))) {
			if (logger.isDebugEnabled()) {
				logger.debug("Does not know about designatorType: "
						+ designatorType);
			}
			return new EvaluationResult(
					BagAttribute.createEmptyBag(attributeType));
		}

		Set<String> allowedAttributes = attributes.get(designatorType)
				.getAttributeNames();
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

		logger.info("Total time for non-CDR call: "
				+ (System.currentTimeMillis() - totalTime) + " milliseconds");

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
		String resourceParts[] = resourceID.split("/");
		Set<String> results;

		// either the last part is the pid, or the last-but one is the pid and
		// the last is the datastream
		// if we have a pid, we query on that, if we have a datastream we query
		// on the datastream
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

		logger.debug("Getting attribute for resource " + subject);

		// the different types of RI attribute specification...
		// if there is no "query" option for the attribute
		String query = attributes.get(designatorType).get(attribute)
				.get("query");
		if (query == null) {
			// it's a simple relationship lookup
			// see if a relationship is specified, otherwise default to the
			// attribute name URI
			String relationship = attributes.get(designatorType).get(attribute)
					.get("relationship");
			if (relationship == null) {
				relationship = attribute; // default to use attribute URI as
											// relationship if none specified
			}

			// see if we are querying based on the resource (object, datstream
			// etc) or just on the object (pid)
			String target = attributes.get(designatorType).get(attribute)
					.get("target");
			String queryTarget;
			if (target != null && target.equals("object")) {
				queryTarget = pid;
			} else {
				queryTarget = subject;
			}

			Map<String, Set<String>> relationships;

			try {
				logger.debug("Getting attribute using relationship "
						+ relationship);
				relationships = relationshipResolver.getRelationships(
						queryTarget, relationship);
			} catch (MelcoeXacmlException e) {
				throw new AttributeFinderException(e.getMessage(), e);
			}

			if (relationships == null || relationships.isEmpty()) {
				return new EvaluationResult(BagAttribute.createEmptyBag(type));
			}

			// there will only be results for one attribute, this will get all
			// the values
			results = relationships.get(relationship);

		} else {
			// get the language and query output variable
			String queryLang = attributes.get(designatorType).get(attribute)
					.get("queryLang"); // language
			String variable = attributes.get(designatorType).get(attribute)
					.get("value"); // query text
			String resource = attributes.get(designatorType).get(attribute)
					.get("resource"); // resource marker in query
			String object = attributes.get(designatorType).get(attribute)
					.get("object"); // object/pid marker in query

			String subjectURI = "info:fedora/" + subject;
			String pidURI = "info:fedora/" + pid;

			// replace the resource marker in the query with the subject
			if (resource != null) {
				query = query.replace(resource, subjectURI);
			}
			// and the pid/object marker
			if (object != null) {
				query = query.replace(object, pidURI);
			}

			// run it
			try {
				logger.debug("Using a " + queryLang
						+ " query to get attribute " + attribute);
				results = relationshipResolver.getAttributesFromQuery(query,
						queryLang, variable);
			} catch (MelcoeXacmlException e) {
				throw new AttributeFinderException(e.getMessage(), e);
			}
		}

		Set<AttributeValue> bagValues = new HashSet<AttributeValue>();
		logger.debug("Attribute values found: " + results.size());
		for (String s : results) {
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