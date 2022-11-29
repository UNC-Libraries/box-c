package edu.unc.lib.boxc.operations.api.order;

import java.util.List;

/**
 * Validator for a order request object
 * @author bbpennel
 */
public interface OrderValidator {
    /**
     * Get whether an order request was valid or not
     * @return true if the order request was valid
     */
    boolean isValid();

    /**
     * Get the list of validation errors
     * @return List of validation errors, or an empty list if there were no errors
     */
    List<String> getErrors();
}
