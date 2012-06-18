/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.processing.CatchUpService;
import edu.unc.lib.dl.cdr.services.processing.EnhancementConductor;
import edu.unc.lib.dl.fedora.PID;

/**
 * Generates JSON views of the catchup queue and provides RESTful access to it and its operations
 * 
 * @author bbpennel
 * 
 */
@Controller
@RequestMapping(value = { "/catchup*", "/catchup" })
public class CatchupConductorRestController extends AbstractServiceConductorRestController {
	private static final Logger log = LoggerFactory.getLogger(CatchupConductorRestController.class);

	public static final String BASE_PATH = "/rest/catchup/";
	private static int NUM_RESULTS_PER_SERVICE = 30;

	@Resource
	private EnhancementConductor enhancementConductor;
	@Resource
	private CatchUpService catchUpService;

	private Map<String, String> serviceNameLookup;

	@PostConstruct
	public void init() {
		serviceNameLookup = new HashMap<String, String>();
		for (ObjectEnhancementService service : catchUpService.getServices()) {
			serviceNameLookup.put(service.getClass().getName(), service.getName());
		}
	}

	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getStatus() {
		Map<String, Object> result = new HashMap<String, Object>();

		Map<String, Object> uris = new HashMap<String, Object>();
		result.put("uris", uris);
		uris.put("candidates", BASE_PATH + "candidates/");

		// Status
		result.put("enabled", catchUpService.isEnabled());
		result.put("active", catchUpService.isActive());
		result.put("itemsProcessed", catchUpService.getItemsProcessed());
		result.put("itemsProcessedThisSession", catchUpService.getItemsProcessedThisSession());
		List<Object> servicesList = new ArrayList<Object>();
		result.put("services", servicesList);
		for (ObjectEnhancementService service : catchUpService.getServices()) {
			String unqualifiedName = getUnqualifiedName(service);

			Map<String, Object> serviceRecord = new HashMap<String, Object>();
			serviceRecord.put("serviceName", service.getName());
			serviceRecord.put("className", service.getClass().getName());
			serviceRecord.put("unqualifiedName", unqualifiedName);
			try {
				serviceRecord.put("count", service.countCandidateObjects());
				serviceRecord.put("active", service.isActive());
			} catch (EnhancementException e) {
				log.error("Could not determine if service " + service.getClass().getName() + " was active.", e);
			}
			servicesList.add(serviceRecord);

			uris.put(unqualifiedName, BASE_PATH + "candidates/" + unqualifiedName);
		}

		return result;
	}

	@RequestMapping(value = { "/candidates", "/candidates/" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> serviceCatchUpLists() {
		return getCatchUpCandidates(null, NUM_RESULTS_PER_SERVICE);
	}

	@RequestMapping(value = { "/candidates/{serviceName}" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> serviceCatchUpList(@PathVariable("serviceName") String serviceName) {
		return getCatchUpCandidates(serviceName, NUM_RESULTS_PER_SERVICE);
	}

	public Map<String, ? extends Object> getCatchUpCandidates(String serviceName, int maxResults) {
		Map<String, Object> result = new HashMap<String, Object>();

		for (ObjectEnhancementService service : catchUpService.getServices()) {
			String unqualifiedName = getUnqualifiedName(service);

			if (serviceName == null || unqualifiedName.equals(serviceName)) {
				try {
					List<PID> candidates = service.findCandidateObjects(maxResults);
					result.put(unqualifiedName, candidates);

					// If a service was specified, then we're done
					if (serviceName != null) {
						return result;
					}
				} catch (EnhancementException e) {
					log.error("Failed to retrieve candidates for " + service.getClass().getName(), e);
					result.put(unqualifiedName, "Failed to retrieve candidates for " + unqualifiedName);
				}
			}
		}
		return result;
	}

	private String getUnqualifiedName(Object object) {
		String unqualifiedName = object.getClass().getName();
		try {
			unqualifiedName = unqualifiedName.substring(unqualifiedName.lastIndexOf(".") + 1);
		} catch (IndexOutOfBoundsException e) {
			// Class was in the root package, no need to substring.
		}
		return unqualifiedName;
	}

	public EnhancementConductor getEnhancementConductor() {
		return enhancementConductor;
	}

	public void setEnhancementConductor(EnhancementConductor enhancementConductor) {
		this.enhancementConductor = enhancementConductor;
	}

	public CatchUpService getCatchUpService() {
		return catchUpService;
	}

	public void setCatchUpService(CatchUpService catchUpService) {
		this.catchUpService = catchUpService;
	}
}
