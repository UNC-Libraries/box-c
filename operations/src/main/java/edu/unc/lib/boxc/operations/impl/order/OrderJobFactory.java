package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;

/**
 * Factory which constructs jobs for updating ordering of objects
 *
 * @author bbpennel
 */
public class OrderJobFactory {
    private RepositoryObjectFactory repositoryObjectFactory;
    private RepositoryObjectLoader repositoryObjectLoader;

    /**
     * @param request
     * @return new ordering job runnable
     */
    public Runnable createJob(OrderRequest request) {
        switch(request.getOperation()) {
        case SET: {
            var job = new SetOrderJob();
            job.setRepositoryObjectFactory(repositoryObjectFactory);
            job.setRepositoryObjectLoader(repositoryObjectLoader);
            job.setRequest(request);
            return job;
        }
        case CLEAR: {
            var job = new ClearOrderJob();
            job.setRepositoryObjectFactory(repositoryObjectFactory);
            job.setRepositoryObjectLoader(repositoryObjectLoader);
            job.setRequest(request);
            return job;
        }
        case REMOVE_FROM: {
            var job = new RemoveFromOrderJob();
            job.setRepositoryObjectFactory(repositoryObjectFactory);
            job.setRepositoryObjectLoader(repositoryObjectLoader);
            job.setRequest(request);
            return job;
        }
        default:
            throw new UnsupportedOperationException("Unable to determine job type for request " + request);
        }
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }
}
