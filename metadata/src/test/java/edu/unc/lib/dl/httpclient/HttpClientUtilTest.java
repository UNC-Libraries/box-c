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
package edu.unc.lib.dl.httpclient;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for HttpClientUtil class.
 */
public class HttpClientUtilTest {

    @Test
    public void testGetAuthenticationScope() {
    Map<String, Integer> urlsAndPorts = new HashMap<String, Integer>();
    urlsAndPorts.put("http://www.unc.edu", 80);
    urlsAndPorts.put("https://www.unc.edu", 443);
    urlsAndPorts.put("http://www.unc.edu:5143", 5143);
    urlsAndPorts.put("https://www.unc.edu:17", 17);
    for (Map.Entry<String, Integer> entry : urlsAndPorts.entrySet()) {
        AuthScope scope = HttpClientUtil.getAuthenticationScope(entry.getKey());
        Assert.assertEquals(entry.getValue().intValue(), scope.getPort());
    }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAuthenticationBadURL() {
    HttpClientUtil.getAuthenticationScope("call me snake");
    }

    @Test(expected = NullPointerException.class)
    public void testGetAuthenticationNullURL() {
    HttpClientUtil.getAuthenticationScope(null);
    }

}
