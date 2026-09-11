package edu.unc.lib.boxc.deposit.tdb;

import edu.unc.lib.boxc.deposit.impl.model.DepositModelManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author bbpennel
 */
public class CompactTdbServiceTest {
    private DepositModelManager depositModelManager;
    private CompactTdbService service;

    @BeforeEach
    void setUp() {
        depositModelManager = mock(DepositModelManager.class);
        service = new CompactTdbService();
        service.setDepositModelManager(depositModelManager);
    }

    @Test
    void testCompactTdb_InvokesCompactDataset() {
        service.compactTdb();

        verify(depositModelManager).compactDataset();
    }

    @Test
    void testCompactTdb_ExceptionDoesNotPropagate() {
        doThrow(new RuntimeException("compaction failed")).when(depositModelManager).compactDataset();

        assertDoesNotThrow(() -> service.compactTdb());
    }
}
