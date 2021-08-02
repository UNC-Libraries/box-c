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
package edu.unc.lib.boxc.deposit.utils;

import static java.lang.String.format;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * External resource resolver which throws an exception any time a resource is resolved, to prevent injection attacks.
 *
 * @author bbpennel
 *
 */
public class NoResolutionResourceResolver implements LSResourceResolver {

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        throw new RuntimeException(format("Rejecting resolution of resource: %s|%s|%s|%s|%s",
                type, namespaceURI, publicId, baseURI, systemId));
    }
}
