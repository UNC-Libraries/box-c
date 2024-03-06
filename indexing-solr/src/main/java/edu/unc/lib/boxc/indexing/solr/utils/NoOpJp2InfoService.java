package edu.unc.lib.boxc.indexing.solr.utils;

import java.nio.file.Path;

/**
 * Jp2InfoService for environments where no implementation is available for retrieving jp2 info
 *
 * @author bbpennel
 */
public class NoOpJp2InfoService implements Jp2InfoService {
    @Override
    public Jp2Info getDimensions(Path path) {
        return new Jp2Info();
    }
}
