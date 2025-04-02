package edu.unc.lib.boxc.web.admin.controllers.view;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
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

    protected AbstractUrlBasedView buildView(String viewName) {
        LOG.debug("In DCR View Resolver:" + viewName + " to base view: " + baseView);
        this.getAttributesMap().put("contentPage", subViewPrefix + viewName + this.getSuffix());
        try {
            return super.buildView(baseView);
        } catch (Exception e) {
            throw new RepositoryException("Failed to build view for " + viewName, e);
        }
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
