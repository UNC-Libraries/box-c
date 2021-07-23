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
package edu.unc.lib.boxc.deposit.api;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Packaging Types
 * @author bbpennel
 *
 */
public enum PackagingType {
    METS_CDR("http://cdr.unc.edu/METS/profiles/Simple"),
    SIMPLE_OBJECT("http://cdr.unc.edu/model/Simple"),
    SIMPLE_ZIP("http://purl.org/net/sword/terms/SimpleZip"),
    ATOM("http://purl.org/net/sword/terms/Atom"),
    BAG_WITH_N3("http://cdr.unc.edu/BAGIT/profiles/N3"),
    BAGIT("http://purl.org/net/sword/package/BagIt"),
    DIRECTORY("http://cdr.unc.edu/DirectoryIngest"),
    BXC3_TO_5_MIGRATION("https://library.unc.edu/dcr/packaging/bxc3To5Migration");

    private String uri;

    PackagingType(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return this.uri;
    }

    public URI toURI() {
        try {
            return new URI(this.uri);
        } catch (URISyntaxException e) {
            throw new Error("Unexpected", e);
        }
    }

    public boolean equals(String value) {
        return this.uri.equals(value);
    }

    @Override
    public String toString() {
        return this.uri;
    }

    public static PackagingType getPackagingType(String uri) {
        for (PackagingType type: values()) {
            if (type.getUri().equals(uri)) {
                return type;
            }
        }

        return null;
    }
}
