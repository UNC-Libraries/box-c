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
package edu.unc.lib.dl.xml;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;

/**
 * Process MODS
 * @author count0
 *
 */
public class ModsXmlHelper {
    private static final String _stylesheetPackage = "/edu/unc/lib/dl/schematron/";
    private static final Log log = LogFactory.getLog(ModsXmlHelper.class);

    private static Templates mods2dc = null;
    private static Templates dcterms2MODS = null;

    private ModsXmlHelper() {
    }

    private static Source mods2dcsrc = new StreamSource(
            ModsXmlHelper.class.getResourceAsStream(_stylesheetPackage
                    + "MODS3-22simpleDC.xsl"));

    private static Source dcterms2MODSSrc = new StreamSource(
            ModsXmlHelper.class.getResourceAsStream(_stylesheetPackage
                    + "dc2MODS/dcterms2MODS.xsl"));

    static {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            // set a Resolver that can look in the classpath
            factory.setURIResolver(new URIResolver() {
                public Source resolve(final String href, final String base)
                        throws TransformerException {
                    Source result = null;
                    result = new StreamSource(ModsXmlHelper.class
                            .getResourceAsStream(_stylesheetPackage + href));
                    return result;
                }
            });

            mods2dc = factory.newTemplates(mods2dcsrc);
            dcterms2MODS = factory.newTemplates(dcterms2MODSSrc);
        } catch (TransformerFactoryConfigurationError e) {
            log.error("Error setting up transformer factory.", e);
            throw new Error("Error setting up transformer factory", e);
        } catch (TransformerConfigurationException e) {
            log.error("Error setting up transformer.", e);
            throw new Error("Error setting up transformer", e);
        }
    }

    public static String getFormattedLabelText(final Element mods) {
        String result = null;
        try {
            Document dc = transform(mods);
            result = dc.getRootElement().getChildText("title",
                    JDOMNamespaceUtil.DC_NS);
        } catch (TransformerException e) {
            log.error(
                    "Cannot get label from MODS due to DC transform failure.",
                    e);
        } catch (IllegalStateException e) {
            log.error(
                    "Cannot get label from MODS due to DC transform failure.",
                    e);
        }
        return result;
    }

    public static Document transform(final Element mods) throws TransformerException {
        Source modsSrc = new JDOMSource(mods);
        JDOMResult dcResult = new JDOMResult();
        Transformer t = null;
        try {
            t = mods2dc.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new Error("There was a problem configuring the transformer.",
                    e);
        }
        t.transform(modsSrc, dcResult);
        return dcResult.getDocument();
    }

    public static Document transformDCTerms2MODS(final Element dcterms)
            throws TransformerException {
        Source dctermsSrc = new JDOMSource(dcterms);
        JDOMResult modsResult = new JDOMResult();
        Transformer t = null;
        try {
            t = dcterms2MODS.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new Error("There was a problem configuring the transformer.",
                    e);
        }
        t.transform(dctermsSrc, modsResult);
        return modsResult.getDocument();
    }
}
