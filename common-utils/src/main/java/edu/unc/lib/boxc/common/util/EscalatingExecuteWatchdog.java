package edu.unc.lib.boxc.common.util;

import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Watchdog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Custom ExecuteWatchdog that escalates to SIGKILL if the process does not terminate after receiving SIGTERM
 * @author krwong
 */
public class EscalatingExecuteWatchdog extends ExecuteWatchdog {
    private static final Logger log = LoggerFactory.getLogger(EscalatingExecuteWatchdog.class);
    private static final Duration ESCALATION_DELAY = Duration.ofSeconds(2);

    private Process monitoredProcess;

    private EscalatingExecuteWatchdog(Duration timeout) {
        super(timeout.toMillis());
    }

    public static EscalatingExecuteWatchdog create(Duration timeout) {
        return new EscalatingExecuteWatchdog(timeout);
    }

    @Override
    public synchronized void start(final Process processToMonitor) {
        this.monitoredProcess = processToMonitor;
        super.start(processToMonitor);
    }

    @Override
    public synchronized void timeoutOccured(final Watchdog w) {
        // Call parent first to set killedProcess flag and do initial SIGTERM/destroy()
        super.timeoutOccured(w);

        if (monitoredProcess != null && monitoredProcess.isAlive()) {
            log.debug("Process survived SIGTERM, waiting {}ms before escalating", ESCALATION_DELAY.toMillis());

            try {
                boolean exited = monitoredProcess.waitFor(ESCALATION_DELAY.toMillis(),
                        java.util.concurrent.TimeUnit.MILLISECONDS);
                if (!exited) {
                    log.warn("Process did not respond to SIGTERM, escalating to SIGKILL");
                    monitoredProcess.destroyForcibly();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for process termination, forcing kill");
                monitoredProcess.destroyForcibly();
            }
        }
    }
}
