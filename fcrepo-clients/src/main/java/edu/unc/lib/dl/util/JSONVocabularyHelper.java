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
package edu.unc.lib.dl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.map.ObjectMapper;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import edu.unc.lib.dl.xml.VocabularyHelper;

/**
 * Vocabulary helper instantiated from a JSON array containing a list of vocabulary terms
 *
 * @author bbpennel
 * @date Oct 7, 2015
 */
public class JSONVocabularyHelper implements VocabularyHelper {

    private List<String> values;

    private String vocabularyURI;
    private String invalidTermPrefix;

    private String selectorString;
    private XPathExpression<Element> selector;
    private Namespace[] namespaces;

    @Override
    public List<List<String>> getAuthoritativeForm(String term) {
        if (values.contains(term)) {
            return Arrays.asList(Arrays.asList(term));
        }
        return null;
    }

    @Override
    public List<List<String>> getAuthoritativeForms(Element docRoot) throws JDOMException {
        List<List<String>> terms = new ArrayList<>();

        List<Element> elements = selector.evaluate(docRoot);
        for (Element element : elements) {
            String value = element.getTextTrim();
            if (values.contains(value)) {
                terms.add(Arrays.asList(value));
            }
        }

        return terms;
    }

    @Override
    public Set<String> getInvalidTerms(Element docRoot) throws JDOMException {
        return generateInvalidTermsList(docRoot, false);
    }

    @Override
    public Set<String> getInvalidTermsWithPrefix(Element docRoot) throws JDOMException {
        return generateInvalidTermsList(docRoot, true);
    }

    private Set<String> generateInvalidTermsList(Element docRoot, boolean includePrefix) {
        Set<String> invalidTerms = new HashSet<>();

        List<Element> elements = selector.evaluate(docRoot);
        for (Element element : elements) {
            String value = element.getTextTrim();
            if (!values.contains(value)) {
                if (includePrefix) {
                    invalidTerms.add(invalidTermPrefix + "|" + value);
                } else {
                    invalidTerms.add(value);
                }
            }
        }

        return invalidTerms;
    }

    @Override
    public boolean updateDocumentTerms(Element docElement) throws JDOMException {
        return false;
    }

    @Override
    public Collection<String> getVocabularyTerms() {
        return values;
    }

    @Override
    public void setSelector(String selector) {
        XPathFactory xFactory = XPathFactory.instance();
        this.selector = xFactory.compile(selector, Filters.element(), null, namespaces);
        this.selectorString = selector;
    }

    @Override
    public void setSelectorNamespaces(Namespace[] namespaces) {
        this.namespaces = namespaces;
    }

    @Override
    public String getSelector() {
        return selectorString;
    }

    public void setInvalidTermPrefix(String prefix) {
        this.invalidTermPrefix = prefix;
    }

    @Override
    public String getInvalidTermPrefix() {
        return invalidTermPrefix;
    }

    @Override
    public String getVocabularyURI() {
        return this.vocabularyURI;
    }

    @Override
    public void setVocabularyURI(String vocabularyURI) {
        this.vocabularyURI = vocabularyURI;
    }

    @Override
    public void setContent(byte[] content) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String[] values = mapper.readValue(content, String[].class);
        this.values = Arrays.asList(values);
    }
}
