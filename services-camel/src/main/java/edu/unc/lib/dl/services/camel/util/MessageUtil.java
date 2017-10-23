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
package edu.unc.lib.dl.services.camel.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.Message;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * Utilities for working with event messages
 *
 * @author bbpennel
 *
 */
public class MessageUtil {

    private MessageUtil() {
    }

    /**
     * Returns the body of the given message represented as a XML Document
     *
     * @param msg
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    public static Document getDocumentBody(Message msg) throws JDOMException, IOException {
        Object body = msg.getBody();

        if (body != null) {
            if (body instanceof Document) {
                return (Document) body;
            } else if (body instanceof InputStream) {
                SAXBuilder bodyBuilder = new SAXBuilder();
                return bodyBuilder.build((InputStream) body);
            } else if (body instanceof String) {
                SAXBuilder bodyBuilder = new SAXBuilder();
                return bodyBuilder.build(
                        new ByteArrayInputStream(((String) body).getBytes()));
            }
        }

        return null;
    }
}
