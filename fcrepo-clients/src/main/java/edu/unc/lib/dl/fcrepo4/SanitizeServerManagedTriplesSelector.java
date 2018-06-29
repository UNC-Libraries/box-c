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
package edu.unc.lib.dl.fcrepo4;

import static edu.unc.lib.dl.rdf.Fcrepo4Repository.created;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.createdBy;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.lastModified;
import static edu.unc.lib.dl.rdf.Fcrepo4Repository.lastModifiedBy;
import static edu.unc.lib.dl.rdf.IanaRelation.describedby;
import static edu.unc.lib.dl.rdf.Premis.hasMessageDigest;
import static edu.unc.lib.dl.rdf.Premis.hasSize;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.rdf.Ldp;

/**
 * Selector which returns all triples which are not fcrepo server managed
 * triples.
 *
 * @author bbpennel
 *
 */
public class SanitizeServerManagedTriplesSelector extends SimpleSelector {

    @Override
    public boolean selects(Statement s) {
        return !isServerManaged(s);
    }

    private static boolean isServerManaged(Statement s) {
        return (s.getPredicate().getNameSpace().equals(Fcrepo4Repository.NS) && !relaxedPredicate(s.getPredicate()))
                || s.getPredicate().equals(describedby)
                || s.getPredicate().equals(hasMessageDigest)
                || s.getPredicate().equals(hasSize)
                || (s.getPredicate().equals(RDF.type) && forbiddenType(s.getResource()));
    }

    private static boolean forbiddenType(final Resource resource) {
        return resource.getNameSpace().equals(Fcrepo4Repository.NS)
            || resource.getURI().equals(Ldp.Container.getURI())
            || resource.getURI().equals(Ldp.NonRdfSource.getURI())
            || resource.getURI().equals(Ldp.RdfSource.getURI());
   }

    private static boolean relaxedPredicate(final Property p) {
        return (p.equals(created) || p.equals(createdBy)
                || p.equals(lastModifiedBy) || p.equals(lastModified));
    }
}
