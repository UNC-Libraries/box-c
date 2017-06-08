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
package edu.unc.lib.dl.cdr.sword.server;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Category;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.swordapp.server.OriginalDeposit;
import org.swordapp.server.ResourcePart;
import org.swordapp.server.Statement;
import org.swordapp.server.UriRegistry;

/**
 * 
 * @author bbpennel
 *
 */
public class AtomStatementImpl extends Statement {
    private String author;
    private String feedUri;
    private String title;
    private String updated;

    public AtomStatementImpl(String feedUri, String author, String title, String updated) {
        this.contentType = "application/atom+xml;type=feed";
        this.author = author != null ? author : "Unknown";
        this.feedUri = feedUri;
        this.title = title != null ? title : "Untitled";
        this.updated = updated;
    }

    @Override
    public void writeTo(Writer out) throws IOException {
        Abdera abdera = new Abdera();
        Feed feed = abdera.newFeed();

        feed.setId(this.feedUri);
        feed.addLink(this.feedUri, "self");
        feed.setTitle(this.title);
        feed.addAuthor(this.author);

        if (this.updated != null) {
            feed.setUpdated(this.updated);
        } else {
            feed.setUpdated(new Date());
        }

        // create an entry for each Resource Part
        for (ResourcePart resource : this.resources) {
            Entry entry = feed.addEntry();

            // id
            // summary
            // title
            // updated
            entry.setContent(new IRI(resource.getUri()), resource.getMediaType());
            entry.setId(resource.getUri());
            entry.setTitle("Resource " + resource.getUri());
            entry.setSummary("Resource Part");
            entry.setUpdated(new Date());
        }

        // create an entry for each original deposit
        for (OriginalDeposit deposit : this.originalDeposits) {
            Entry entry = feed.addEntry();

            // id
            // summary
            // title
            // updated
            entry.setId(deposit.getUri());
            entry.setTitle("Original Deposit " + deposit.getUri());
            entry.setSummary("Original Deposit");
            entry.setUpdated(new Date());

            if (deposit.getMediaType() != null) {
                entry.setContent(new IRI(deposit.getUri()), deposit.getMediaType());
            }
            entry.addCategory(UriRegistry.SWORD_TERMS_NAMESPACE, UriRegistry.SWORD_ORIGINAL_DEPOSIT,
                    "Original Deposit");
            if (deposit.getDepositedOn() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                entry.addSimpleExtension(new QName(UriRegistry.SWORD_TERMS_NAMESPACE, "depositedOn"),
                        sdf.format(deposit.getDepositedOn()));
            }

            if (deposit.getDepositedOnBehalfOf() != null) {
                entry.addSimpleExtension(new QName(UriRegistry.SWORD_TERMS_NAMESPACE, "depositedOnBehalfOf"),
                        deposit.getDepositedOnBehalfOf());
            }

            if (deposit.getDepositedBy() != null) {
                entry.addSimpleExtension(new QName(UriRegistry.SWORD_TERMS_NAMESPACE, "depositedBy"),
                        deposit.getDepositedBy());
            }

            if (deposit.getPackaging() != null) {
                for (String packaging : deposit.getPackaging()) {
                    entry.addSimpleExtension(UriRegistry.SWORD_PACKAGING, packaging);
                }
            }
        }

        // now at the state as a categories
        for (String state : this.states.keySet()) {
            Category cat = feed.addCategory(UriRegistry.SWORD_STATE, state, "State");
            if (this.states.get(state) != null) {
                cat.setText(this.states.get(state));
            }
        }

        // now write the feed
        feed.writeTo(out);
    }
}
