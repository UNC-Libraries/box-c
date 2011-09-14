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
package edu.unc.lib.dl.ui.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * Extension of InternalResourceViewResolver which performs very basic tiling by resolving
 * the view name provided to be the content panel for the specified base view page.
 * @author bbpennel
 */
public class CDRViewResolver extends InternalResourceViewResolver {
	private static final Logger LOG = LoggerFactory.getLogger(CDRViewResolver.class);
	
	protected String baseView;
	protected String subViewPrefix;
	
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		LOG.debug("In CDR View Resolver:" + viewName);
		this.getAttributesMap().put("contentPage", subViewPrefix + viewName + this.getSuffix());
		return super.buildView(baseView);
	}

	public String getBaseView() {
		return baseView;
	}

	public void setBaseView(String baseView) {
		this.baseView = baseView;
	}

	public String getSubViewPrefix() {
		return subViewPrefix;
	}

	public void setSubViewPrefix(String subViewPrefix) {
		this.subViewPrefix = subViewPrefix;
	}
}
