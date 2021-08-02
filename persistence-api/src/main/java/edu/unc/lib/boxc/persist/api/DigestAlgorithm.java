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
package edu.unc.lib.boxc.persist.api;

import org.apache.jena.rdf.model.Property;

import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;

/**
 * Digest algorithms supported by bxc
 *
 * @author bbpennel
 */
public enum DigestAlgorithm {
    SHA1("sha1", CdrDeposit.sha1sum),
    MD5("md5", CdrDeposit.md5sum);

    public static final DigestAlgorithm DEFAULT_ALGORITHM = DigestAlgorithm.SHA1;

    private final String name;
    private final Property depositProp;

    private DigestAlgorithm(String name, Property depositProp) {
        this.depositProp = depositProp;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Property getDepositProperty() {
        return depositProp;
    }

    public static DigestAlgorithm getByDepositProperty(Property depositProp) {
        for (DigestAlgorithm alg : values()) {
            if (alg.depositProp.equals(depositProp)) {
                return alg;
            }
        }
        return null;
    }
}
