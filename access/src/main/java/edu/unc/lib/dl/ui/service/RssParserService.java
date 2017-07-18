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
package edu.unc.lib.dl.ui.service;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import edu.unc.lib.dl.ui.model.response.RssFeedBean;

/**
 * Retrieves and processes RSS feeds
 * 
 * @author bbpennel
 */
public class RssParserService extends XMLRetrievalService {
    private static final Logger LOG = LoggerFactory.getLogger(RssParserService.class);
    private static final Namespace CONTENT =
            Namespace.getNamespace("content", "http://purl.org/rss/1.0/modules/content/");

    public static RssFeedBean getRssFeed(String url) throws Exception {
        return getRssFeed(url, -1);
    }

    public static RssFeedBean getRssFeed(String url, int maxResults) throws Exception {
        try {
            Document document = getXMLDocument(url);
            return buildRssFeed(document, maxResults);
        } catch (Exception e) {
            // if we get an error trying to build from a non-valid uri,
            // just return an empty LookupTable
            LOG.error("Failed to retrieve RSS feed " + url, e);
        }
        return null;
    }

    private static RssFeedBean buildRssFeed(Document document, int maxResults) throws Exception {
        RssFeedBean rssFeed = new RssFeedBean();
        Element rootElement = null;
        Element channelElement = null;
        String rssFeedVersion = null;

        if (null == document) {
            throw new Exception("Empty document");
        }

        rootElement = document.getRootElement();
        if (!"rss".equalsIgnoreCase(rootElement.getName())) {
            throw new Exception("Invalid XML");
        }

        rssFeedVersion = rootElement.getAttributeValue("version");
        channelElement = rootElement.getChild("channel");
        if (null == channelElement) {
            throw new Exception("Empty feed");
        }

        // Getting the feed contents
        rssFeed = getHeader(channelElement);
        rssFeed.setVersion(rssFeedVersion);
        addFeedItems(channelElement, rssFeed, maxResults);

        return (rssFeed);
    }

    private static RssFeedBean getHeader(org.jdom2.Element channelElement) {
        // Sets the RSS feed header information to the
        // RssFeedBean object
        RssFeedBean rssFeed = new RssFeedBean();
        rssFeed.setTitle(getValueOfChildElement(channelElement, "title"));
        rssFeed.setLink(getValueOfChildElement(channelElement, "link"));
        rssFeed.setDescription(getValueOfChildElement(channelElement, "description"));
        return rssFeed;
    }

    // Get the child node value
    private static String getValueOfChildElement(Element parentElement, String tagName, Namespace ns) {
        Element childElement = null;
        String tagValue = null;
        childElement = parentElement.getChild(tagName, ns);
        tagValue = (null != childElement) ? childElement.getValue().trim() : null;
        return (tagValue);
    }

    private static String getValueOfChildElement(Element parentElement, String tagName) {
        return getValueOfChildElement(parentElement, tagName, null);
    }

    private static RssFeedBean.RssItem.EnclosureObject getEnclosure(Element parentElement) {
        Element childElement = null;

        childElement = parentElement.getChild("enclosure");
        if (childElement == null) {
            return null;
        }
        RssFeedBean.RssItem.EnclosureObject enclosureObject = new RssFeedBean.RssItem.EnclosureObject();
        enclosureObject.setUrl(childElement.getAttribute("url").getValue());
        enclosureObject.setType(childElement.getAttribute("type").getValue());
        enclosureObject.setLength(childElement.getAttribute("length").getValue());
        return enclosureObject;
    }

    @SuppressWarnings("unchecked")
    private static void addFeedItems(Element channelElement, RssFeedBean rssFeed, int maxResults) {
        List<Element> itemElements = null;
        RssFeedBean.RssItem anRssItem = null;

        itemElements = channelElement.getChildren("item");
        if (null != itemElements) {
            if (maxResults == -1) {
                maxResults = itemElements.size();
            }
            for (int i = 0; i < maxResults; i++) {
                Element anItemElement = itemElements.get(i);
                anRssItem = new RssFeedBean.RssItem();
                anRssItem.setTitle(getValueOfChildElement(anItemElement, "title"));
                anRssItem.setLink(getValueOfChildElement(anItemElement, "link"));
                anRssItem.setDescription(getValueOfChildElement(anItemElement, "description"));
                anRssItem.setPubDate(getValueOfChildElement(anItemElement, "pubDate"));
                anRssItem.setGuid(getValueOfChildElement(anItemElement, "guid"));
                anRssItem.setEncoded(getValueOfChildElement(anItemElement, "encoded", CONTENT));
                anRssItem.setEnclosure(getEnclosure(anItemElement));
                rssFeed.items.add(anRssItem);
            }
        }
    }
}
