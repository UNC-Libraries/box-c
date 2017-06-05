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

import java.io.OutputStream;
import java.io.StringWriter;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

/**
 * Convenience methods for working with SOAP
 * @author count0
 *
 */
public class SOAPUtil {
    // private static final Log log = LogFactory.getLog(SOAPUtil.class);
    private SOAPUtil() {
    }

    public static void print(SOAPMessage msg, OutputStream out) {
        StreamResult result = new StreamResult(out);
        print(msg, result);
    }

    public static String getString(SOAPMessage msg) {
        StringWriter w = new StringWriter();
        StreamResult result = new StreamResult(w);
        print(msg, result);
        w.flush();
        return w.toString();
    }

    private static void print(SOAPMessage msg, StreamResult result) {
        try {
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            Source sourceContent = msg.getSOAPPart().getContent();
            // StreamResult result = new StreamResult(out);
            transformer.transform(sourceContent, result);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        }
    }
}
