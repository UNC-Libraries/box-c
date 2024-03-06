package edu.unc.lib.boxc.indexing.solr.utils;

import java.nio.file.Path;

/**
 * A service which extracts information about JPEG 2000 images
 *
 * @author bbpennel
 */
public interface Jp2InfoService {
    /**
     * Gets dimension info about the provided jp2 file
     * @param path Path of the jp2 file
     * @return Jp2Info object populated with dimension details
     */
    Jp2Info getDimensions(Path path);
}
