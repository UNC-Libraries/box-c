package edu.unc.lib.boxc.deposit.tdb;

import edu.unc.lib.boxc.deposit.impl.model.DepositModelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Service which triggers compaction of the deposit TDB2 dataset on a schedule.
 *
 * @author bbpennel
 */
@Component
@EnableScheduling
public class CompactTdbService {
    private static final Logger log = LoggerFactory.getLogger(CompactTdbService.class);

    private DepositModelManager depositModelManager;

    @Scheduled(cron = "${deposits.tdb.compact.cron:0 0 3 * * *}")
    public void compactTdb() {
        log.info("Starting scheduled compaction of the deposit TDB2 dataset");
        try {
            depositModelManager.compactDataset();
            log.info("Finished scheduled compaction of the deposit TDB2 dataset");
        } catch (Exception e) {
            log.error("Failed to compact the deposit TDB2 dataset", e);
        }
    }

    /**
     * @param depositModelManager the deposit model manager to set
     */
    public void setDepositModelManager(DepositModelManager depositModelManager) {
        this.depositModelManager = depositModelManager;
    }
}
