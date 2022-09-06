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
package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.utils.MemberOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter which sets the member order. Updates the memberOrderId field.
 *
 * @author bbpennel
 */
public class SetMemberOrderFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetMemberOrderFilter.class);

    private MemberOrderService memberOrderService;

    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        log.debug("Performing SetMemberOrderFilter for object {}", dip.getPid());
        var orderId = memberOrderService.getOrderValue(dip.getContentObject());
        var doc = dip.getDocument();
        doc.setMemberOrderId(orderId);
    }

    public void setMemberOrderService(MemberOrderService memberOrderService) {
        this.memberOrderService = memberOrderService;
    }
}
