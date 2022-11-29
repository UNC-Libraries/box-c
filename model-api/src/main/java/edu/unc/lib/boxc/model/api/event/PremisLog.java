package edu.unc.lib.boxc.model.api.event;

import org.apache.jena.rdf.model.Model;

/**
 * A readable PREMIS log object
 *
 * @author bbpennel
 */
public interface PremisLog {
    /**
     * @return a model containing the events in this log.
     */
    public Model getEventsModel();
}
