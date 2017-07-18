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
package edu.unc.lib.dl.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolve path URIs
 * @author count0
 *
 */
public class ClasspathURIResolver implements URIResolver {
    private static Logger log = LoggerFactory
            .getLogger(ClasspathURIResolver.class);
    private List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();

    public ClasspathURIResolver(@SuppressWarnings("rawtypes") Class... loaders) {
        for (@SuppressWarnings("rawtypes")
        Class l : loaders) {
            classLoaders.add(l.getClassLoader());
        }
    }

    public ClasspathURIResolver() {
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        log.debug("href=" + href + " : base=" + base);
        String fileName = null;
        try {
            int k = href.lastIndexOf('/') + 1;
            if (k > 0) {
                fileName = href.substring(k);
            } else {
                fileName = href;
            }
            InputStream input = null;
            for (ClassLoader l : classLoaders) {
                input = l.getResourceAsStream(fileName);
                if (input != null) {
                    break;
                }
            }
            if (input == null) {
                input = ClassLoader.getSystemResourceAsStream(fileName);
            }

            if (input != null) {
                return (new StreamSource(input));
            }
        } catch (Exception ie) {
            log.error("File " + fileName + " not found!", ie);
        }

        // instruct the caller to use the default lookup
        return (null);
    }

}
