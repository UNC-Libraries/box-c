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
package edu.unc.lib.deposit;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.unc.lib.deposit.work.DepositSupervisor;

/**
 * 
 * @author count0
 *
 */
public class DepositDaemon implements Daemon {
    private static final Logger LOG = LoggerFactory.getLogger(DepositDaemon.class);
    private AbstractApplicationContext appContext;
    public DepositDaemon() {
    }

    @Override
    public void destroy() {
        LOG.info("Deposit Daemon destroy called");
        appContext.destroy();
        // supervisor has destroy hooks registered in appContext
    }

    @Override
    public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
    }

    @Override
    public void start() throws Exception {
        LOG.info("Starting the Deposit Daemon");
        if (appContext == null) {
            appContext = new ClassPathXmlApplicationContext(
                    new String[] {"service-context.xml", "deposit-jobs-context.xml", "fcrepo-clients-context.xml"});
            appContext.registerShutdownHook();
        } else {
            appContext.refresh();
        }
        // start the supervisor
        DepositSupervisor supervisor = appContext.getBean(DepositSupervisor.class);
        supervisor.start();
        LOG.info("Started the Deposit Daemon");
    }

    @Override
    public void stop() throws Exception {
        LOG.info("Stopping the Deposit Daemon");
        // stop the supervisor
        DepositSupervisor supervisor = appContext.getBean(DepositSupervisor.class);
        supervisor.stop();
        appContext.stop();
        LOG.info("Stopped the Deposit Daemon");
    }

}
