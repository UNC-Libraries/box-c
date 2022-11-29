package edu.unc.lib.boxc.indexing.solr.utils;

import java.util.Date;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.Namespace;

import edu.unc.lib.boxc.common.util.DateTimeUtil;

/**
 * Convenience methods for working with JDOM
 * @author bbpennel
 *
 */
public class JDOMQueryUtil {
    private JDOMQueryUtil() {

    }

    public static Element getElementByAttribute(final List<?> elements,
            final String attribute, final String value) {
        return getElementByAttribute(elements, attribute, null, value);
    }

    public static Element getElementByAttribute(final List<?> elements,
            final String attribute, final Namespace attributeNS, final String value) {
        for (Object elementObj : elements) {
            Element element = (Element) elementObj;
            String attrValue = attributeNS == null ? element
                    .getAttributeValue(attribute) : element.getAttributeValue(
                    attribute, attributeNS);
            if (attrValue == value
                    || (value != null && value.equals(attrValue))) {
                return element;
            }
        }
        return null;
    }

    public static Element getChildByAttribute(final Element parent, final String childName,
            final Namespace namespace, final String attribute, final String value) {
        List<?> elements;
        if (childName != null) {
            if (namespace != null) {
                elements = parent.getChildren(childName, namespace);
            } else {
                elements = parent.getChildren(childName);
            }
        } else {
            elements = parent.getChildren();
        }
        return getElementByAttribute(elements, attribute, value);
    }

    public static Date parseISO6392bDateChild(final Element parent, final String childName,
            final Namespace namespace) {
        String dateString = parent.getChildText(childName, namespace);
        if (dateString != null) {
            try {
                return DateTimeUtil.parseUTCToDate(dateString);
            } catch (IllegalArgumentException e) {
                // Wasn't a valid date, ignore it.
            }
        }
        return null;
    }
}
