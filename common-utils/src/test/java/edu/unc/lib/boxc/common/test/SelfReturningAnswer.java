package edu.unc.lib.boxc.common.test;

import static org.mockito.Mockito.RETURNS_DEFAULTS;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Mockito answer which returns the mocked object for all unstubbed method calls.
 *
 */
public class SelfReturningAnswer implements Answer<Object> {

    public Object answer(InvocationOnMock invocation) throws Throwable {
        Object mock = invocation.getMock();

        if (invocation.getMethod().getReturnType().isInstance(mock)) {
            return mock;
        } else {
            return RETURNS_DEFAULTS.answer(invocation);
        }
    }
}
