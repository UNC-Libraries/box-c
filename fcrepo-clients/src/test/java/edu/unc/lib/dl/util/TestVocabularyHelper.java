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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;

import edu.unc.lib.dl.xml.VocabularyHelper;

public class TestVocabularyHelper implements VocabularyHelper {

    private Set<String> invalidTerms;
    private String vocabURI;
    private String prefix;

    @Override
    public List<List<String>> getAuthoritativeForm(String term) {
        return null;
    }

    @Override
    public Set<String> getInvalidTerms(Element docElement) throws JDOMException {
        return invalidTerms;
    }

    public void setInvalidTerms(Set<String> invalidTerms) {
        this.invalidTerms = invalidTerms;
    }

    @Override
    public Set<String> getInvalidTermsWithPrefix(Element modsRoot) throws JDOMException {
        Set<String> prefixed = new HashSet<>(invalidTerms.size());
        for (String term : invalidTerms) {
            prefixed.add(prefix + "|" + term);
        }
        return prefixed;
    }

    @Override
    public boolean updateDocumentTerms(Element docElement) throws JDOMException {
        return false;
    }

    @Override
    public Collection<String> getVocabularyTerms() {
        return null;
    }

    @Override
    public void setSelector(String selector) {

    }

    @Override
    public String getInvalidTermPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String getVocabularyURI() {
        return vocabURI;
    }

    @Override
    public void setVocabularyURI(String vocabularyURI) {
        this.vocabURI = vocabularyURI;
    }

    @Override
    public void setContent(byte[] content) throws Exception {
    }

    @Override
    public List<List<String>> getAuthoritativeForms(Element docElement) throws JDOMException {
        return null;
    }

    @Override
    public void setSelectorNamespaces(Namespace[] namespaces) {
    }

    @Override
    public String getSelector() {
        return null;
    }
}