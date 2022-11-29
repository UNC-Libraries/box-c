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
