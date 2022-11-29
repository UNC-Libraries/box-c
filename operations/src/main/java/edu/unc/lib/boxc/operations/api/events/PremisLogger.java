package edu.unc.lib.boxc.operations.api.events;

import java.io.InputStream;
import java.util.Date;

import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.boxc.model.api.event.PremisLog;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Logs and provides access to PREMIS events for a repository object
 *
 * @author bbpennel
 *
 */
public interface PremisLogger extends PremisLog, AutoCloseable {

    /**
     * Returns an instance of a PremisEventBuilder with the provided event pid and date.
     * @param eventPid
     * @param eventType
     * @param date timestamp of the event
     * @return PremisEventBuilder
     */
    public PremisEventBuilder buildEvent(PID eventPid, Resource eventType, Date date);

    /**
     * Returns an instance of buildEvent with the timestamp automatically set to the current time
     * @param eventType
     * @return PremisEventBuilder
     */
    public PremisEventBuilder buildEvent(Resource eventType);

    /**
     * Adds events to this log
     *
     * @param eventResc
     * @return
     */
    public PremisLogger writeEvents(Resource... eventResc);

    /**
     * Creates the log for this logger from the given inputstream if
     * it does not exist.
     *
     * @param contentStream contents of the log
     * @return this logger
     */
    public PremisLogger createLog(InputStream contentStream);

    /**
     * Closes the logger
     */
    @Override
    void close();

    /**
     * @return true if the logger has been closed
     */
    boolean isClosed();
}
