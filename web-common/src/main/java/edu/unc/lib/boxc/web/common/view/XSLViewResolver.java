package edu.unc.lib.boxc.web.common.view;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.web.common.exceptions.RenderViewException;

/**
 * 
 * @author count0
 *
 */
public class XSLViewResolver {
    private static final Logger LOG = LoggerFactory.getLogger(XSLViewResolver.class);

    Map<String,XSLComponentView> views;

    public XSLViewResolver() {
        views = new HashMap<String,XSLComponentView>();
    }

    public void setViews(Map<String,String> views) {
        for (Map.Entry<String, String> viewPair: views.entrySet()) {
            try {
                XSLComponentView view = new XSLComponentView(viewPair.getValue());
                if (view != null) {
                    view.initializeTransformer();
                    this.views.put(viewPair.getKey(), view);
                }
            } catch (Exception e) {
                LOG.error("Failed to construct XSLComponentView " + viewPair.getKey() +
                        " from source " + viewPair.getValue(), e);
            }
        }
    }

    public void refreshViews() {
        for (XSLComponentView view: views.values()) {
            try {
                view.initializeTransformer();
            } catch (Exception e) {
                LOG.error("Failed to refresh XSLComponentView from source " + view.getSource(), e);
            }
        }
    }

    public String renderView(String key, Element doc, Map<String,Object> parameters) throws RenderViewException {
        if (key == null || !views.containsKey(key)) {
            throw new RenderViewException("The view " + key + " was requested but is not bound");
        }

        try {
            return views.get(key).renderView(doc, parameters);
        } catch (TransformerException e) {
            throw new RenderViewException("Failed to transform the document to the specified view " + key, e);
        }
    }

    public String renderView(String key, Element doc) throws RenderViewException {
        return renderView(key, doc, null);
    }

    public String renderView(String key, Document doc) throws RenderViewException {
        return renderView(key, doc.getRootElement(), null);
    }
}
