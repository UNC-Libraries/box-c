package edu.unc.lib.boxc.deposit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import edu.unc.lib.boxc.deposit.work.DepositSupervisor;

/**
 * Detects when the application's context has fully initialized and then kicks off the deposit supervisor.
 *
 * @author bbpennel
 *
 */
public class SpringDepositServiceInitiator implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(SpringDepositServiceInitiator.class);

    private DepositSupervisor depositSupervisor;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Context initialized, starting deposit service");

        depositSupervisor.start();
    }

    /**
     * @param depositSupervisor the depositSupervisor to set
     */
    public void setDepositSupervisor(DepositSupervisor depositSupervisor) {
        this.depositSupervisor = depositSupervisor;
    }
}
