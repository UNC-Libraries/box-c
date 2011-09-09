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
/**
 *
 */
package edu.unc.lib.dl.util;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Gregory Jansen
 *
 */
public class DateTimeUtilTest extends Assert {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link edu.unc.lib.dl.util.DateTimeUtil#parseISO8601(java.lang.String)}.
     */
    @Test
    public void testParseISO8601() {
	String test = "2008-05-04T14:24:18-05:00";
	DateTime dt = DateTimeUtil.parseISO8601toUTC(test);
	System.err.println("Is this UTC for "+test+"? "+dt);

	test = "200805";
	dt = DateTimeUtil.parseISO8601toUTC(test);
	System.err.println("Is this UTC for "+test+"? "+dt);

	test = "2008-05";
	dt = DateTimeUtil.parseISO8601toUTC(test);
	System.err.println("Is this UTC for "+test+"? "+dt);
    }

}
