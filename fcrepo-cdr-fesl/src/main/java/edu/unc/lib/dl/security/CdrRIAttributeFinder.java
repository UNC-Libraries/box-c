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
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.fcrepo.server.security.xacml.MelcoeXacmlException;
import org.fcrepo.server.security.xacml.pdp.finder.AttributeFinderConfigUtil;
import org.fcrepo.server.security.xacml.pdp.finder.AttributeFinderException;
import org.fcrepo.server.security.xacml.util.ContextUtil;
import org.fcrepo.server.security.xacml.util.RelationshipResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.StandardAttributeFactory;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.finder.AttributeFinderModule;

import edu.unc.lib.dl.fedora.AccessControlUtils;
import edu.unc.lib.dl.util.TripleStoreQueryServiceMulgaraImpl;

public class CdrRIAttributeFinder extends AttributeFinderModule {

	private static final Logger log = LoggerFactory
			.getLogger(CdrRIAttributeFinder.class);

	private AttributeFactory attributeFactory = null;

	private RelationshipResolver relationshipResolver = null;

	private Map<Integer, Set<String>> attributes = null;

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
			log.info("Initialised AttributeFinder:"
					+ this.getClass().getName());

			if (log.isDebugEnabled()) {
				log.debug("registering the following attributes: ");
				for (Integer k : attributes.keySet()) {
					for (String l : attributes.get(k)) {
						log.debug(k + ": " + l);
					}
				}
			}

			Map<String, String> resolverConfig = AttributeFinderConfigUtil
					.getResolverConfig(this.getClass().getName());
			if (log.isDebugEnabled()) {
				for (String s : resolverConfig.keySet()) {
					log.debug(s + ": " + resolverConfig.get(s));
				}
			}

			relationshipResolver = ContextUtil.getInstance()
					.getRelationshipResolver();

			attributeFactory = StandardAttributeFactory.getFactory();

			Map<String, String> options = AttributeFinderConfigUtil
					.getOptionMap(this.getClass().getName());

			String roleString = options.get("cdr.roles");
			String rolePrefix = options.get("cdr.role.prefix");
			String tripleStorePrefix = options.get("cdr.role.triplestore.prefix");

			String[] roles = roleString.split(" ");

			log.debug("loadSettingsFromOptions: roleString: " + roleString);
			log.debug("loadSettingsFromOptions: rolePrefix: " + rolePrefix);
			log.debug("loadSettingsFromOptions: tripleStorePrefix: " + tripleStorePrefix);

			// load roles from config
			Properties accessControlProperties = new Properties();
			for (String role : roles) {
				log.debug("loadSettingsFromOptions: role: " + role);
				String permissionString = options.get(rolePrefix + role);
				accessControlProperties.put(tripleStorePrefix + role, permissionString);
			}

			// load caching settings and tripleStore
			String username = options.get("cdr.username");
			String password = options.get("cdr.password");
			String itqlEndpointURL = options.get("cdr.itql.endpoint.url");
			String serverModelUri = options.get("cdr.server.model.uri");
			int cacheDepth = convertStringToIntOtherwiseReturnZero(options.get("cdr.cache.depth"));
			int cacheLimit = convertStringToIntOtherwiseReturnZero(options.get("cdr.cache.limit"));
			int cacheResetTime = convertStringToIntOtherwiseReturnZero(options.get("cdr.cache.reset.time.hours"));

			TripleStoreQueryServiceMulgaraImpl tripleStoreQueryService = new TripleStoreQueryServiceMulgaraImpl();
			tripleStoreQueryService.setName(username); // fedoraAdmin
			tripleStoreQueryService.setPass(password); // inst1repo
			tripleStoreQueryService.setItqlEndpointURL(itqlEndpointURL); // "http://nagin:8080/webservices/services/ItqlBeanService"
			tripleStoreQueryService.setServerModelUri(serverModelUri); // "rmi://nagin/server1#"

			// Load CDR configuration
			accessControlUtils = new AccessControlUtils();
			accessControlUtils.setAttributeFactory(attributeFactory);
			accessControlUtils.setOnlyCacheReadPermissions(false);
			accessControlUtils.setTripleStoreQueryService(tripleStoreQueryService);
			accessControlUtils.setCacheDepth(cacheDepth);
			accessControlUtils.setCacheLimit(cacheLimit);
			accessControlUtils.setCacheResetTime(cacheResetTime);
			accessControlUtils.init();
			accessControlUtils.startCacheCleanupThreadForFedoraBasedAccessControl();

		} catch (AttributeFinderException afe) {
			log.error("Attribute finder not initialised:"
					+ this.getClass().getName(), afe);
		}
	}

	private int convertStringToIntOtherwiseReturnZero(String value) {
		int result = 0;

		try {
			result = Integer.parseInt(value);
		} catch (Exception e) {
			return 0;
		}

		return result;
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
		return attributes.keySet();
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
		if (log.isDebugEnabled()) {
			log.debug("CdrRIAttributeFinder: [" + attributeType.toString()
					+ "] " + attributeId + ", rid=" + resourceId);
		}

		if (resourceId == null || resourceId.equals("")) {
			return new EvaluationResult(BagAttribute
					.createEmptyBag(attributeType));
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

				return accessControlUtils.processCdrAccessControl(temp, attrName, attributeType);
			}
		}
		// we only know about registered attributes from config file
		if (!attributes.keySet().contains(new Integer(designatorType))) {
			if (log.isDebugEnabled()) {
				log.debug("Does not know about designatorType: "
						+ designatorType);
			}
			return new EvaluationResult(BagAttribute
					.createEmptyBag(attributeType));
		}

		Set<String> allowedAttributes = attributes.get(new Integer(
				designatorType));
		if (!allowedAttributes.contains(attrName)) {
			if (log.isDebugEnabled()) {
				log.debug("Does not know about attribute: " + attrName);
			}
			return new EvaluationResult(BagAttribute
					.createEmptyBag(attributeType));
		}

		EvaluationResult result = null;
		try {
			result = getEvaluationResult(resourceId, attrName, attributeType);
		} catch (Exception e) {
			log.error("Error finding attribute: " + e.getMessage(), e);
			return new EvaluationResult(BagAttribute
					.createEmptyBag(attributeType));
		}

		log.info("Total time for non-CDR call: "
				+ (System.currentTimeMillis() - totalTime) + " milliseconds");

		return result;
	}

    /**
    *
    * @param resourceID - the hierarchical XACML resource ID
    * @param attribute - attribute to get
    * @param type
    * @return
    * @throws AttributeFinderException
    */
   private EvaluationResult getEvaluationResult(String resourceID,
                                                String attribute,
                                                URI type)
           throws AttributeFinderException {

       // split up the path of the hierarchical resource id
       String resourceParts[] = resourceID.split("/");

       // either the last part is the pid, or the last-but one is the pid and the last is the datastream
       // if we have a pid, we query on that, if we have a datastream we query on the datastream
       String subject;
       if (resourceParts.length > 1) {
           if (resourceParts[resourceParts.length - 1].contains(":")) { // ends with a pid, we have pid only
               subject = resourceParts[resourceParts.length - 1];
           } else { // datastream
               String pid = resourceParts[resourceParts.length - 2];
               subject = pid + "/" + resourceParts[resourceParts.length - 1];
           }
       } else {
           // eg /FedoraRepository, not a valid path to PID or PID/DS
           log.debug("Resource ID not valid path to PID or datastream: " + resourceID);
           return new EvaluationResult(BagAttribute.createEmptyBag(type));
       }


       Map<String, Set<String>> relationships;
       // FIXME: this is querying for all relationships, and then filtering the one we want
       // better to query directly on the one we want (but currently no public method on relationship resolver to do this)
       try {
           log.debug("Getting relationships for " + subject);
           relationships = relationshipResolver.getRelationships(subject);
       } catch (MelcoeXacmlException e) {
           throw new AttributeFinderException(e.getMessage(), e);
       }

       Set<String> results = relationships.get(attribute);
       if (results == null || results.size() == 0) {
           return new EvaluationResult(BagAttribute.createEmptyBag(type));
       }

       Set<AttributeValue> bagValues = new HashSet<AttributeValue>();
       for (String s : results) {
           AttributeValue attributeValue = null;
           try {
               attributeValue = attributeFactory.createValue(type, s);
           } catch (Exception e) {
               log.error("Error creating attribute: " + e.getMessage(), e);
               continue;
           }

           bagValues.add(attributeValue);

           if (log.isDebugEnabled()) {
               log.debug("AttributeValue found: [" + type.toASCIIString()
                       + "] " + s);
           }
       }

       BagAttribute bag = new BagAttribute(type, bagValues);

       return new EvaluationResult(bag);
   }


/*	private EvaluationResult getEvaluationResult(String pid, String attribute,
			URI type) throws AttributeFinderException {
		Map<String, Set<String>> relationships;
		try {
			relationships = relationshipResolver.getRelationships(pid);
		} catch (MelcoeXacmlException e) {
			throw new AttributeFinderException(e.getMessage(), e);
		}
		Set<String> results = relationships.get(attribute);
		if (results == null || results.size() == 0) {
			return new EvaluationResult(BagAttribute.createEmptyBag(type));
		}

		Set<AttributeValue> bagValues = new HashSet<AttributeValue>();
		for (String s : results) {
			AttributeValue attributeValue = null;
			try {
				attributeValue = attributeFactory.createValue(type, s);
			} catch (Exception e) {
				log.error("Error creating attribute: " + e.getMessage(), e);
				continue;
			}

			bagValues.add(attributeValue);

			if (log.isDebugEnabled()) {
				log.debug("AttributeValue found: [" + type.toASCIIString()
						+ "] " + s);
			}
		}

		BagAttribute bag = new BagAttribute(type, bagValues);

		return new EvaluationResult(bag);
	} */
}
