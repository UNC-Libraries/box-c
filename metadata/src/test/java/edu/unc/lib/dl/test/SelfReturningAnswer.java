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
package edu.unc.lib.dl.test;

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
