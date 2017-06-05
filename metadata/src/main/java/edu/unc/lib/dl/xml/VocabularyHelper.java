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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;

/**
 * @author bbpennel
 * @date Sep 30, 2014
 */
public interface VocabularyHelper {

    /**
     * Returns the authoritative version of the term provided. May return
     * multiple matching forms, and each may contain multiple parts
     *
     * @param term
     * @return
     */
    public List<List<String>> getAuthoritativeForm(String term);

    /**
     * Returns a list of authoritative versions of all selected terms from the
     * give document
     *
     * @param docElement
     * @return
     * @throws JDOMException
     */
    public List<List<String>> getAuthoritativeForms(Element docElement)
            throws JDOMException;

    /**
     * Determines a set of invalid terms in the provided xml document root
     * element.
     *
     * @param docElement
     * @return
     * @throws JDOMException
     */
    public Set<String> getInvalidTerms(Element docElement) throws JDOMException;

    /**
     * Determines a set of prefixed invalid terms in the provided xml document root
     * element.
     * 
     * @param modsRoot
     * @return
     * @throws JDOMException
     */
    public Set<String> getInvalidTermsWithPrefix(Element modsRoot)
            throws JDOMException;

    /**
     * Locates invalid terms in the given document and replaces them with
     * authoritative terms when possible
     *
     * @param docElement
     * @return true if any modifications were made to the given document
     * @throws JDOMException
     */
    public boolean updateDocumentTerms(Element docElement) throws JDOMException;

    /**
     * Returns the terms in this vocabulary as a list
     *
     * @return
     */
    public Collection<String> getVocabularyTerms();

    /**
     * Set namespace for a selector
     * 
     * @param namespaces
     */
    public void setSelectorNamespaces(Namespace[] namespaces);

    /**
     * Get a selector
     */
    public String getSelector();

    /**
     * Set a selector
     * 
     * @param selector
     */
    public void setSelector(String selector);

    /**
     * Get the name of the triple prefix used for storing invalid terms from
     * this vocabulary
     *
     * @return
     */
    public String getInvalidTermPrefix();

    /**
     * Get the URI used to identify this vocabulary
     *
     * @return
     */
    public String getVocabularyURI();

    /**
     * Set URI for a vocabulary
     * 
     * @param vocabularyURI
     */
    public void setVocabularyURI(String vocabularyURI);

    /**
     * Store the body of content for this vocabulary
     *
     * @param content
     * @throws Exception
     */
    public void setContent(byte[] content) throws Exception;
}
