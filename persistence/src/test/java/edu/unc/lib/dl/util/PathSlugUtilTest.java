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
package edu.unc.lib.dl.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PathSlugUtilTest extends Assert {
    private static final Log log = LogFactory.getLog(PathSlugUtilTest.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCheckValidPathString() {
	assertTrue("Good path", PathUtil.isValidPathString("/foo/bar/canine/feline/bovine"));
	assertTrue("Good path", PathUtil.isValidPathString("REPOSITORY/foo/bar/canine/feline/bovine"));
	assertTrue("Good path", PathUtil.isValidPathString("/foo/bar/canine/feline/eating-habits"));
	assertTrue("Good path", PathUtil.isValidPathString("/foo/bar/canine/feline/eating-habits.txt"));
	assertFalse("Bad path", PathUtil.isValidPathString("//my/dog/has/fleas"));
	assertFalse("Bad path", PathUtil.isValidPathString("/what/the/*&@# "));
	assertFalse("Bad path", PathUtil.isValidPathString("/over/slashed/"));
	assertFalse("Bad path", PathUtil.isValidPathString("floating/pavilion/philosophers/path"));
	assertFalse("Bad path", PathUtil.isValidPathString("/_the_low_road_"));
    }

    @Test
    public void testIncrementSlug() {
	String test = "  ---My\\/Pictures (2008): the story.    ";
	String slug = PathUtil.makeSlug(test);
	String islug = PathUtil.incrementSlug(test);
	String islugdup = PathUtil.incrementSlug(slug);
	log.info("obtained incremented slug:" + islug + " for test string: " + test);
	assertTrue("Output must match expected string", "---My_Pictures_2008_the_story._1".equals(islug));
	assertTrue("Output should be the same whether or not it gets slug form input.", islugdup.equals(islug));

	String islug2 = PathUtil.incrementSlug(islug);
	log.info("Incremented a second time:" + islug2);
	assertTrue("Output must match expected string", "---My_Pictures_2008_the_story._2".equals(islug2));
    }

    @Test
    public void testMakeSlug() {
	String test = "---My\\/Pictures (2008): the story.txt";
	String slug = PathUtil.makeSlug(test);
	log.info("obtained slug:" + slug + " for test string: " + test);
	assertTrue("Output must match expected string", "---My_Pictures_2008_the_story.txt".equals(slug));
    }

}
