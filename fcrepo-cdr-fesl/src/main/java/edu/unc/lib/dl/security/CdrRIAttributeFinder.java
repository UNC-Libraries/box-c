/**
 * Copyright © 2008 The University of North Carolina at Chapel Hill (cdr@unc.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.fcrepo.server.security.xacml.pdp.finder.AttributeFinderException;
import org.fcrepo.server.security.xacml.pdp.finder.attribute.DesignatorAttributeFinderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import org.jboss.security.xacml.sunxacml.EvaluationCtx;
import org.jboss.security.xacml.sunxacml.attr.AttributeFactory;
import org.jboss.security.xacml.sunxacml.attr.AttributeValue;
import org.jboss.security.xacml.sunxacml.attr.BagAttribute;
import org.jboss.security.xacml.sunxacml.attr.StringAttribute;
import org.jboss.security.xacml.sunxacml.attr.StandardAttributeFactory;
import org.jboss.security.xacml.sunxacml.cond.EvaluationResult;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;

public class CdrRIAttributeFinder extends DesignatorAttributeFinderModule {

	private static final Logger log = LoggerFactory.getLogger(CdrRIAttributeFinder.class);

	URI embargoUntil = ContentModelHelper.CDRProperty.embargoUntil.getURI();
	URI dataAccessCategory = ContentModelHelper.CDRProperty.dataAccessCategory.getURI();
	URI userRole = ContentModelHelper.CDRProperty.userRole.getURI();
	URI isPublished = ContentModelHelper.CDRProperty.isPublished.getURI();
	URI isActive = ContentModelHelper.CDRProperty.isActive.getURI();
	static URI datastreamIdAttribute = null;
	static URI fedoraSubjectRoleAttribute = null;
	static URI stringDataType = null;
	static URI accessSubjectCategory = null;
	static {
		try {
			fedoraSubjectRoleAttribute = new URI("urn:fedora:names:fedora:2.1:subject:role");
			stringDataType = new URI("http://www.w3.org/2001/XMLSchema#string");
			datastreamIdAttribute = new URI("urn:fedora:names:fedora:2.1:resource:datastream:id");
			accessSubjectCategory = new URI("urn:oasis:names:tc:xacml:1.0:subject-category:access-subject");
		} catch (URISyntaxException e) {
			throw new Error(e);
		}
	}

	private AttributeFactory attributeFactory = null;

	private AccessControlUtils accessControlUtils;

	public CdrRIAttributeFinder() {
		super();
		attributeFactory = StandardAttributeFactory.getFactory();
	}

	public void init() {
		log.info("Initialised AttributeFinder: {}", this.getClass().getName());
		if (log.isDebugEnabled()) {
			log.debug("registering the following attributes: ");
			for (Integer desNum : this.getSupportedDesignatorTypes()) {
				log.debug("Designator Type: {}", desNum);
				for (String attrName : m_attributes.get(desNum).keySet()) {
					log.debug("\t{}", attrName);
				}
			}
		}
	}

	/**
	 * Used to get an attribute. If one of those values isn't being asked for, or if the types are wrong, then an empty
	 * bag is returned.
	 *
	 * @param attributeType
	 *           the datatype of the attributes to find, which must be time, date, or dateTime for this module to resolve
	 *           a value
	 * @param attributeId
	 *           the identifier of the attributes to find, which must be one of the three ENVIRONMENT_* fields for this
	 *           module to resolve a value
	 * @param issuer
	 *           the issuer of the attributes, or null if unspecified
	 * @param subjectCategory
	 *           the category of the attribute or null, which ignored since this only handles non-subjects
	 * @param context
	 *           the representation of the request data
	 * @param designatorType
	 *           the type of designator
	 * @return the result of attribute retrieval, which will be a bag with a single attribute, an empty bag, or an error
	 */
	@Override
	public EvaluationResult findAttribute(URI attributeType, URI attributeId, URI issuer, URI subjectCategory,
			EvaluationCtx context, int designatorType) {

		long startTime = System.currentTimeMillis();

		String resourceId = context.getResourceId().encode();
		if (log.isDebugEnabled()) {
			log.debug("CdrRIAttributeFinder: [" + attributeType.toString() + "] " + attributeId + ", rid=" + resourceId);
		}

		if (resourceId == null || resourceId.equals("")) {
			return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
		}

		if (resourceId.equals("/FedoraRepository")) {
			return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
		}

		// figure out which attribute we're looking for
		String attrName = attributeId.toString();

		// we only know about registered attributes from config file
		if (!getSupportedDesignatorTypes().contains(new Integer(designatorType))) {
			if (log.isDebugEnabled()) {
				log.debug("Does not know about designatorType: {}", designatorType);
			}
			return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
		}

		Set<String> allowedAttributes = m_attributes.get(designatorType).keySet();
		if (!allowedAttributes.contains(attrName)) {
			if (log.isDebugEnabled()) {
				log.debug("Does not know about attribute: {}", attrName);
			}
			return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
		}

		EvaluationResult result = null;
		try {
			result = getEvaluationResult(resourceId, attributeId, context, designatorType, attributeType);
		} catch (Exception e) {
			log.error("Error finding attribute: " + e.getMessage(), e);
			return new EvaluationResult(BagAttribute.createEmptyBag(attributeType));
		}

		log.info("Total time for CDR role lookup: {} milliseconds", (System.currentTimeMillis() - startTime));
		return result;
	}

	private Set<String> getShibbolethGroups(EvaluationCtx context) {
		Set<String> groups = new HashSet<String>();
		
		EvaluationResult result = context.getSubjectAttribute(stringDataType, fedoraSubjectRoleAttribute, accessSubjectCategory);
		
		if (result.indeterminate()) {
			return groups;
		}
		
		if (result.getStatus() != null) {
			return groups;
		}
		
		AttributeValue attributeValue = result.getAttributeValue();
		
		if (attributeValue == null) {
			return groups;
		}
		
		if (!attributeValue.isBag()) {
			return groups;
		}
		
		BagAttribute bag = (BagAttribute) attributeValue;
		Iterator iterator = bag.iterator();
		
		while (iterator.hasNext()) {
			Object value = iterator.next();
			
			if (value instanceof StringAttribute) {
				StringAttribute attribute = (StringAttribute) value;
				log.debug("adding attribute: {}", attribute);
				groups.add(attribute.getValue());
			}
		}
		
		return groups;
	}

	private String getDatastreamID(EvaluationCtx context) {
		EvaluationResult result = context.getResourceAttribute(stringDataType, datastreamIdAttribute, null);
		
		if (result.indeterminate()) {
			return null;
		}
		
		if (result.getStatus() != null) {
			return null;
		}
		
		AttributeValue attributeValue = result.getAttributeValue();
		
		if (attributeValue == null) {
			return null;
		}
		
		if (!attributeValue.isBag()) {
			return null;
		}
		
		BagAttribute bag = (BagAttribute) attributeValue;
		Iterator iterator = bag.iterator();
		
		while (iterator.hasNext()) {
			Object value = iterator.next();
			
			if (value instanceof StringAttribute) {
				StringAttribute attribute = (StringAttribute) value;
				log.debug("returning attribute: {}", attribute);
				return attribute.getValue();
			}
		}
		
		return null;
	}

	/**
	 *
	 * @param resourceID
	 *           - the hierarchical XACML resource ID
	 * @param attribute
	 *           - attribute to get - this is a URI that maps to a Fedora relationship name
	 * @param type
	 * @return
	 * @throws AttributeFinderException
	 */
	private EvaluationResult getEvaluationResult(String resourceID, URI attribute, EvaluationCtx context,
			int designatorType, URI type) throws AttributeFinderException {

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
			log.debug("Resource ID not valid path to PID or datastream: {}", resourceID);
			return new EvaluationResult(BagAttribute.createEmptyBag(type));
		}

		log.debug("Getting attribute {} for resource {}", attribute, pid);

		if (userRole.equals(attribute)) {
			Set<String> groups = getShibbolethGroups(context);
			Set<String> roles = accessControlUtils.getRolesForGroups(groups, new PID(pid));
			return makeStringBagResult(roles, type);
		} else if (embargoUntil.equals(attribute)) {
			List<String> embargoes = getAccessControlUtils().getAllEmbargoes(new PID(pid));
			return makeStringBagResult(embargoes, type);
		} else if (dataAccessCategory.equals(attribute)) {
			String datastreamId = getDatastreamID(context);
			if (datastreamId == null) {
				return new EvaluationResult(BagAttribute.createEmptyBag(type));
			} else {
				List<String> categories = getAccessControlUtils().getDatastreamCategories(datastreamId);
				return makeStringBagResult(categories, type);
			}

		} else if (isPublished.equals(attribute)) {
			List<String> statuses = new ArrayList<String>(1);
			statuses.add(Boolean.toString(accessControlUtils.isPublished(new PID(pid))));
			return makeStringBagResult(statuses, type);

		} else if (isActive.equals(attribute)) {
			List<String> statuses = new ArrayList<String>(1);
			statuses.add(Boolean.toString(accessControlUtils.isActive(new PID(pid))));
			return makeStringBagResult(statuses, type);
		}
		return new EvaluationResult(BagAttribute.createEmptyBag(type));
	}

	private EvaluationResult makeStringBagResult(Collection<String> values, URI type) {
		Set<AttributeValue> bagValues = new HashSet<AttributeValue>();
		for (String s : values) {
			AttributeValue attributeValue = null;
			try {
				attributeValue = attributeFactory.createValue(type, s);
			} catch (Exception e) {
				log.error("Error creating attribute: {}", e.getMessage(), e);
				continue;
			}
			bagValues.add(attributeValue);
		}
		BagAttribute bag = new BagAttribute(type, bagValues);
		return new EvaluationResult(bag);
	}

	public AccessControlUtils getAccessControlUtils() {
		return accessControlUtils;
	}

	public void setAccessControlUtils(AccessControlUtils accessControlUtils) {
		this.accessControlUtils = accessControlUtils;
	}
}