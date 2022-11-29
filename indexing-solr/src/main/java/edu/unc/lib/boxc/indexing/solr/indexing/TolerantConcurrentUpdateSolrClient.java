package edu.unc.lib.boxc.indexing.solr.indexing;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.impl.BinaryResponseParser;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.springframework.util.CollectionUtils;

/**
 * ConcurrentUpdateSolrClient intended for use with an update endpoint with the
 * TolerantUpdateProcessorFactory enabled as a updateRequestProcessorChain. This
 * client will log errors found within partial success update responses.
 *
 * @author bbpennel
 */
public class TolerantConcurrentUpdateSolrClient extends ConcurrentUpdateSolrClient {
    private static final long serialVersionUID = 1L;
    private static final Logger log = getLogger(TolerantConcurrentUpdateSolrClient.class);

    private ResponseParser respParser = new BinaryResponseParser();

    /**
     * @param builder
     */
    protected TolerantConcurrentUpdateSolrClient(Builder builder) {
        super(builder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(HttpResponse httpResp) {
        try {
            // With the tolerant processor enabled, a 200 response can list errors for records that failed to update
            NamedList<Object> resp = respParser.processResponse(httpResp.getEntity().getContent(), "UTF-8");
            NamedList<Object> headers = (NamedList<Object>) resp.get("responseHeader");
            if (headers == null) {
                return;
            }
            List<Object> errors = (List<Object>) headers.get("errors");
            if (CollectionUtils.isEmpty(errors)) {
                return;
            }
            for (Object error : errors) {
                NamedList<Object> details = (NamedList<Object>) error;
                log.error("Failed to commit solr update for {}: {}", details.get("id"), details.get("message"));
            }
        } catch (IOException e) {
            log.error("Failed to parse success response", e);
        }
    }

    // Extended so that it will build a TolerantConcurrentUpdateSolrClient
    public static class Builder extends ConcurrentUpdateSolrClient.Builder {
        /**
         * @param baseSolrUrl
         */
        public Builder(String baseSolrUrl) {
            super(baseSolrUrl);
        }

        @Override
        public ConcurrentUpdateSolrClient build() {
            if (baseSolrUrl == null) {
              throw new IllegalArgumentException("Cannot create HttpSolrClient without a valid baseSolrUrl!");
            }

            return new TolerantConcurrentUpdateSolrClient(this);
          }
    }
}
