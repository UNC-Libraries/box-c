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
package edu.unc.lib.dl.search.solr.tags;

import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.invalidTerm;

import java.util.List;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.Datastream;
import edu.unc.lib.dl.search.solr.model.Tag;
import edu.unc.lib.dl.search.solr.util.FacetConstants;

/**
 * 
 * @author bbpennel
 *
 */
public class DescriptiveTagProvider implements TagProvider {

    @Override
    public void addTags(BriefObjectMetadata record, AccessGroupSet accessGroups) {
        Datastream descr = record.getDatastreamObject("MD_DESCRIPTIVE");
        if (descr != null) {
            record.addTag(new Tag("described"));
        }

        // Invalid vocabulary terms
        if (record.getContentStatus() != null
                && record.getContentStatus().contains(FacetConstants.INVALID_VOCAB_TERM)) {
            Tag invalidTermTag = new Tag("invalid term");

            List<String> invalidTerms = record.getRelation(invalidTerm.getPredicate());
            if (invalidTerms != null) {
                for (String relation : invalidTerms) {
                    invalidTermTag.addDetail(relation);
                }
            }

            record.addTag(invalidTermTag);
        }
    }

}
