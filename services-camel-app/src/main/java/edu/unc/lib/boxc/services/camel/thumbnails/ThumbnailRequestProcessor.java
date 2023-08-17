package edu.unc.lib.boxc.services.camel.thumbnails;

import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * @author sharonluong
 */
public class ThumbnailRequestProcessor implements Processor {
    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;

    @Override
    public void process(Exchange exchange) throws Exception {
        var in = exchange.getIn();
        var pidString = in.getHeader("file_pid").toString();
        var file = repositoryObjectLoader.getFileObject(PIDs.get(pidString));
        var work = file.getParent();
        // check permission? set up new permission?

        repositoryObjectFactory.createExclusiveRelationship(work, Cdr.useAsThumbnail, file);
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }
}
