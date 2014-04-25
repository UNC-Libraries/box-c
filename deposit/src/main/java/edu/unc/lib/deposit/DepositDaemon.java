package edu.unc.lib.deposit;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.deposit.work.DepositSupervisor;

public class DepositDaemon implements Daemon {
	private AbstractApplicationContext appContext;
	private DaemonContext daemonContext;

	public DepositDaemon() {
	}

	@Override
	public void destroy() {
		// supervisor has destroy hooks registered in appContext
	}

	@Override
	public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
		this.daemonContext = daemonContext;
	}

	@Override
	public void start() throws Exception {
		if(appContext == null) {
			appContext = new ClassPathXmlApplicationContext(new String[] {"service-context.xml"});
			appContext.registerShutdownHook();
		} else {
			appContext.refresh();
		}
		// start the supervisor
		DepositSupervisor supervisor = appContext.getBean(DepositSupervisor.class);
		supervisor.start();
	}

	@Override
	public void stop() throws Exception {
		// stop the supervisor
		DepositSupervisor supervisor = appContext.getBean(DepositSupervisor.class);
		supervisor.stop();
	}

}
