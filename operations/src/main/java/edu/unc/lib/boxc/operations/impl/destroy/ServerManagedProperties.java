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
package edu.unc.lib.boxc.operations.impl.destroy;

import org.apache.jena.rdf.model.Property;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Premis;

/**
 * Contains properties managed by Fedora that we want to store in Tombstones
 *
 * @author harring
 *
 */
public enum ServerManagedProperties {
    DIGEST(Premis.hasMessageDigest),
    SIZE(Premis.hasSize);

    private Property property;

    private ServerManagedProperties(Property property) {
        this.property = property;
    }

    public static boolean isServerManagedProperty(Property p) {
        return p.equals(DIGEST.property) || p.equals(SIZE.property);
    }

    public static Property mapToLocalNamespace(Property p) {
        if (p.equals(DIGEST.property)) {
            return Cdr.hasMessageDigest;
        }
        if (p.equals(SIZE.property)) {
            return Cdr.hasSize;
        }
        return p;
    }
}
