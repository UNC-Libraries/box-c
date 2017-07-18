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
package edu.unc.lib.dl.ui.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * 
 * @author count0
 *
 */
public class XMLRetrievalService {
    protected XMLRetrievalService() {
    }

    public static Document getXMLDocument(String url) throws HttpException, IOException, JDOMException {
        SAXBuilder builder = new SAXBuilder();

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet method = new HttpGet(url);
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(2000)
                .setConnectTimeout(2000)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .build();
        method.setConfig(requestConfig);

        InputStream responseStream = null;
        Document document = null;

        try (CloseableHttpResponse response = client.execute(method)) {
            HttpEntity entity = response.getEntity();
            responseStream = entity.getContent();
            document = builder.build(responseStream);
        }

        return document;
    }
}
