package edu.unc.lib.boxc.operations.impl.vocab;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;

import edu.unc.lib.boxc.operations.api.vocab.VocabularyHelper;

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