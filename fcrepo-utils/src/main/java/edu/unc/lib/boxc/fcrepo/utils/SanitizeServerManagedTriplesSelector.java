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
package edu.unc.lib.boxc.fcrepo.utils;

import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.created;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.createdBy;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.lastModified;
import static edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository.lastModifiedBy;
import static edu.unc.lib.boxc.model.api.rdf.Premis.hasFixity;
import static edu.unc.lib.boxc.model.api.rdf.Premis.hasMessageDigest;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.api.rdf.Memento;

/**
 * Selector which returns all triples which are not fcrepo server managed
 * triples.
 *
 * @author bbpennel
 *
 */
public class SanitizeServerManagedTriplesSelector extends SimpleSelector {
    private boolean allowRelaxed;

    public SanitizeServerManagedTriplesSelector() {
        allowRelaxed = false;
    }

    public SanitizeServerManagedTriplesSelector(boolean allowRelaxed) {
        this.allowRelaxed = allowRelaxed;
    }

    @Override
    public boolean selects(Statement s) {
        return !isServerManaged(s);
    }

    private boolean isServerManaged(Statement s) {
        String ns = s.getPredicate().getNameSpace();
        return (ns.equals(Fcrepo4Repository.NS) && !relaxedPredicate(s.getPredicate()))
                || ns.equals(Memento.NS)
                || s.getPredicate().equals(hasFixity)
                || s.getPredicate().equals(hasMessageDigest)
                || s.getPredicate().equals(Ldp.contains)
                || (s.getPredicate().equals(RDF.type) && forbiddenType(s.getResource()));
    }

    private boolean forbiddenType(final Resource resource) {
        String ns = resource.getNameSpace();
        return ns.equals(Fcrepo4Repository.NS)
                || ns.equals(Memento.NS)
                || ns.equals(Ldp.NS);
   }

    private boolean relaxedPredicate(final Property p) {
        return allowRelaxed && (p.equals(created) || p.equals(createdBy)
                || p.equals(lastModifiedBy) || p.equals(lastModified));
    }
}
