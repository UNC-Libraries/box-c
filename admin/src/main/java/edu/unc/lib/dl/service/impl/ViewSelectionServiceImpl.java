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
package edu.unc.lib.dl.service.impl;

import edu.unc.lib.dl.service.ViewSelectionService;
import edu.unc.lib.dl.util.Constants;

public class ViewSelectionServiceImpl implements ViewSelectionService {

	public String getViewForResourceType(String resourceType) {
		if (resourceType == null) {
			return Constants.VIEW_OBJECT;
		} else if (resourceType.startsWith("Journal")) {
			return getJournalView(resourceType);
		} else if (resourceType.startsWith("Conference")) {
			return getConferenceView(resourceType);
		} else if (resourceType.startsWith("Research")) {
			return getResearchView(resourceType);
		}

		return Constants.VIEW_OBJECT;
	}

	private String getConferenceView(String resourceType) {
		if (resourceType.equals(Constants.RESOURCE_TYPE_CONFERENCE_ITEM)) {
			return Constants.VIEW_CONFERENCE_ITEM;
		} else if (resourceType
				.equals(Constants.RESOURCE_TYPE_CONFERENCE_COLLECTION)) {
			return Constants.VIEW_CONFERENCE_COLLECTION;
		}

		return Constants.VIEW_OBJECT;
	}

	private String getResearchView(String resourceType) {
		if (resourceType.equals(Constants.RESOURCE_TYPE_RESEARCH_ITEM)) {
			return Constants.VIEW_RESEARCH_ITEM;
		} else if (resourceType
				.equals(Constants.RESOURCE_TYPE_RESEARCH_COLLECTION)) {
			return Constants.VIEW_RESEARCH_COLLECTION;
		}

		return Constants.VIEW_OBJECT;
	}

	private String getJournalView(String resourceType) {
		if (resourceType.equals(Constants.RESOURCE_TYPE_JOURNAL_ARTICLE)) {
			return Constants.VIEW_JOURNAL_ARTICLE;
		} else if (resourceType.equals(Constants.RESOURCE_TYPE_JOURNAL_ISSUE)) {
			return Constants.VIEW_JOURNAL_ISSUE;
		} else if (resourceType.equals(Constants.RESOURCE_TYPE_JOURNAL_YEAR)) {
			return Constants.VIEW_JOURNAL_YEAR;
		} else if (resourceType
				.equals(Constants.RESOURCE_TYPE_JOURNAL_CONTENTS)) {
			return Constants.VIEW_JOURNAL_ARTICLE;
		} else if (resourceType
				.equals(Constants.RESOURCE_TYPE_JOURNAL_FRONT_MATTER)) {
			return Constants.VIEW_JOURNAL_ARTICLE;
		} else if (resourceType
				.equals(Constants.RESOURCE_TYPE_JOURNAL_BACK_MATTER)) {
			return Constants.VIEW_JOURNAL_ARTICLE;
		} else if (resourceType
				.equals(Constants.RESOURCE_TYPE_JOURNAL_COLLECTION)) {
			return Constants.VIEW_JOURNAL_COLLECTION;
		}

		return Constants.VIEW_OBJECT;
	}
}
