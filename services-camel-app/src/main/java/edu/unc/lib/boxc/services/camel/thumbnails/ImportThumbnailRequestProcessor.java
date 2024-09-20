package edu.unc.lib.boxc.services.camel.thumbnails;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.thumbnails.ImportThumbnailRequestSerializationHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.io.IOException;

import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

public class ImportThumbnailRequestProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws IOException {
        var in = exchange.getIn();
        var request = ImportThumbnailRequestSerializationHelper.toRequest(in.getBody(String.class));
        var mimetype = request.getMimetype();
        var storagePath = request.getStoragePath();
        var pidString = request.getPidString();

        in.setHeader(CdrBinaryPath, storagePath);
        in.setHeader(CdrBinaryMimeType, mimetype);
        in.setHeader(FCREPO_URI, pidString);
    }
}
