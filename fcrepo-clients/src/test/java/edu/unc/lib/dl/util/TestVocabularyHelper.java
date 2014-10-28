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
import java.util.List;
import java.util.Set;

import org.jdom2.Element;
import org.jdom2.JDOMException;

import edu.unc.lib.dl.xml.VocabularyHelper;

public class TestVocabularyHelper implements VocabularyHelper {

	private Set<String> invalidTerms;
	private String vocabURI;

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
	public String getInvalidTermPredicate() {
		return null;
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
	public List<String> getAuthoritativeForms(Element docElement) throws JDOMException {
		return null;
	}

}