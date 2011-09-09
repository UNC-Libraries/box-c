/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.util;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Allows for runtime management of log levels for all log4j loggers registered to the application's
 * <code>LoggerRepository</code>.  The methods exposed by this class are designed to be friendly to
 * JMX clients, so if an instance of this class is exported
 * by the Spring <code>MBeanExporter</code>, then all of the log4j loggers can be managed via JMX.
 *
 * @author adamc, $Author$
 * @version $Revision$
 */
public class Log4JManager {

    /**
     * Gets the names of the the currently active loggers.
     *
     * @return an array of the names of the currently active loggers.
     */
    public String[] getCurrentLoggers() {
        List<String> loggers = new ArrayList<String>();
        Enumeration en = LogManager.getLoggerRepository().getCurrentLoggers();
        while (en.hasMoreElements()) {
            Logger l = (Logger) en.nextElement();
            loggers.add(l.getName());
        }
        Collections.sort(loggers);
        return loggers.toArray(new String[loggers.size()]);
    }

    /**
     * Gets the current *effective* level of the named logger, or <code>null</code>
     * if no logger by the specified name exists.
     *
     * @param loggerName the name of the logger
     * @return the level of the named logger, or the literal string value
     * <code>"null"</code> if the logger is not found or does not have an explicitly
     * set level.
     */
    public String getLevel(String loggerName) {
        Logger theLogger = LogManager.getLoggerRepository().exists(loggerName);
        if ( theLogger == null ) {
            return null;
        }
        return String.valueOf( theLogger.getEffectiveLevel() );
    }

    /**
     * Sets the level on a specified logger to <code>TRACE</code>
     *
     * @param loggerName the logger to be configured.
     */
    public void activateTraceLevel(String loggerName) {
        setLoggerLevel(loggerName, Level.TRACE);
    }

    /**
     * Sets the level on a specified logger to <code>DEBUG</code>
     *
     * @param loggerName the logger to be configured.
     */
    public void activateDebugLevel(String loggerName) {
        setLoggerLevel(loggerName, Level.DEBUG);
    }

    /**
     * Sets the level on a specified logger to <code>INFO</code>
     *
     * @param loggerName the logger to be configured.
     */
    public void activateInfoLevel(String loggerName) {
        setLoggerLevel(loggerName, Level.INFO);
    }

    /**
     * Sets the level on a specified logger to <code>WARN</code>
     *
     * @param loggerName the logger to be configured.
     */
    public void activateWarnLevel(String loggerName) {
        setLoggerLevel(loggerName, Level.WARN);
    }


    /**
     * Sets the level on a specified logger to <code>ERROR</code>
     *
     * @param loggerName the logger to be configured.
     */
    public void activateErrorLevel(String loggerName) {
        setLoggerLevel(loggerName, Level.ERROR);
    }

    /**
     * Sets the level on a specified logger to <code>FATAL</code>
     *
     * @param loggerName the logger to be configured.
     */
    public void activateFatalLevel(String loggerName) {
        setLoggerLevel(loggerName, Level.FATAL);
    }

    /**
     * Sets the level on a specified logger to <code>OFF</code> (i.e.
     * prevents the logger from emitting any events, but does not
     * remove it from the active logger repository.
     *
     * @param loggerName the logger to be configured.
     */
    public void activateOffLevel(String loggerName) {
        setLoggerLevel(loggerName, Level.OFF);
    }

    /**
     * Gets the appenders for the named logger.
     * @param loggerName the name of the logger.
     * @return an array of strings in the form <code>&lt;name&gt;:&lt;Appender class name&gt;</code>.  The
     * array will be 0-length if the logger cannot be found or has no appenders.
     */
    public String [] getAppenders(String loggerName) {
        Logger l = LogManager.getLoggerRepository().exists(loggerName);
        if ( l == null ) {
            return new String[0];
        }
        List<String> appenders = new ArrayList<String>();
        for(Enumeration en = l.getAllAppenders(); en.hasMoreElements(); ) {
            Appender great = (Appender)en.nextElement();
            appenders.add( great.getName() + ":" + great.getClass().getName());
        }
        return appenders.toArray( new String[appenders.size()]);
    }

    /**
     * Handles setting of logger levels by name, refusing to create
     * logger if it doesn't already exist.
     *
     * @param loggerName the name of the logger
     * @param level the level to be set.
     */
    private void setLoggerLevel(String loggerName, Level level) {
        Logger l = LogManager.getLoggerRepository().exists(loggerName);
        if (l != null) {
            l.setLevel(level);
        }
    }
}
