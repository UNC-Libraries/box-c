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
package edu.unc.lib.boxc.deposit.impl.mets;

/**
 * METS Profile
 * @author bbpennel
 *
 */
public enum METSProfile {
    CDR_SIMPLE("http://cdr.unc.edu/METS/profiles/Simple");

    private String name;

    METSProfile(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean equals(final String name) {
        return this.name.equals(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
