package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job which clears the order of children in a container
 *
 * @author bbpennel
 */
public class ClearOrderJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClearOrderJob.class);

    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private OrderRequest request;

    @Override
    public void run() {
        var parentObject = repositoryObjectLoader.getRepositoryObject(request.getParentPid());
        log.debug("Deleting order property for {}", request.getParentPid());
        repositoryObjectFactory.deleteProperty(parentObject, Cdr.memberOrder);
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setRequest(OrderRequest request) {
        this.request = request;
    }
}
