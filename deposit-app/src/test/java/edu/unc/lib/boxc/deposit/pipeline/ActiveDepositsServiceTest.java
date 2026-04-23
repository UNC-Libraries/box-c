package edu.unc.lib.boxc.deposit.pipeline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ActiveDepositsServiceTest {

    private ActiveDepositsService service;
    private final String DEPOSIT_ID_1 = "deposit1";
    private final String DEPOSIT_ID_2 = "deposit2";
    private final String DEPOSIT_ID_3 = "deposit3";
    private final String DEPOSIT_ID_4 = "deposit4";

    @BeforeEach
    public void setup() {
        service = new ActiveDepositsService();
        service.setMaxConcurrentDeposits(3);
    }

    @Test
    public void testMarkActiveSuccess() {
        boolean result = service.markActive(DEPOSIT_ID_1);

        assertTrue(result);
        assertTrue(service.isDepositActive(DEPOSIT_ID_1));
    }

    @Test
    public void testMarkActiveDuplicate() {
        service.markActive(DEPOSIT_ID_1);

        boolean result = service.markActive(DEPOSIT_ID_1);

        assertFalse(result);
        assertTrue(service.isDepositActive(DEPOSIT_ID_1));
    }

    @Test
    public void testMarkActiveAtMaxCapacity() {
        // Fill to capacity (default is 3)
        service.markActive(DEPOSIT_ID_1);
        service.markActive(DEPOSIT_ID_2);
        service.markActive(DEPOSIT_ID_3);

        boolean result = service.markActive(DEPOSIT_ID_4);

        assertFalse(result);
        assertFalse(service.isDepositActive(DEPOSIT_ID_4));
    }

    @Test
    public void testMarkInactive() {
        service.markActive(DEPOSIT_ID_1);
        assertTrue(service.isDepositActive(DEPOSIT_ID_1));

        service.markInactive(DEPOSIT_ID_1);

        assertFalse(service.isDepositActive(DEPOSIT_ID_1));
    }

    @Test
    public void testMarkInactiveNonExistent() {
        service.markInactive(DEPOSIT_ID_1);

        assertFalse(service.isDepositActive(DEPOSIT_ID_1));
    }

    @Test
    public void testAcceptingNewDepositsWhenEmpty() {
        assertTrue(service.acceptingNewDeposits());
    }

    @Test
    public void testAcceptingNewDepositsWhenPartiallyFull() {
        service.markActive(DEPOSIT_ID_1);
        service.markActive(DEPOSIT_ID_2);

        assertTrue(service.acceptingNewDeposits());
    }

    @Test
    public void testAcceptingNewDepositsWhenAtCapacity() {
        service.markActive(DEPOSIT_ID_1);
        service.markActive(DEPOSIT_ID_2);
        service.markActive(DEPOSIT_ID_3);

        assertFalse(service.acceptingNewDeposits());
    }

    @Test
    public void testIsDepositActiveWhenNotActive() {
        assertFalse(service.isDepositActive(DEPOSIT_ID_1));
    }

    @Test
    public void testMarkActiveAfterInactive() {
        // Fill to capacity
        service.markActive(DEPOSIT_ID_1);
        service.markActive(DEPOSIT_ID_2);
        service.markActive(DEPOSIT_ID_3);

        assertFalse(service.markActive(DEPOSIT_ID_4));

        // Mark one inactive
        service.markInactive(DEPOSIT_ID_1);

        // Should now be able to add a new one
        assertTrue(service.markActive(DEPOSIT_ID_4));
        assertTrue(service.isDepositActive(DEPOSIT_ID_4));
        assertFalse(service.isDepositActive(DEPOSIT_ID_1));
    }
}