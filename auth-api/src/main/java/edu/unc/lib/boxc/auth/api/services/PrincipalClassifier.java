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
package edu.unc.lib.boxc.auth.api.services;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PATRON_PRINC_PATTERN;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper methods for classifying principals by category
 *
 * @author bbpennel
 *
 */
public class PrincipalClassifier {

    private PrincipalClassifier() {
    }

    public static boolean isPatronPrincipal(String principal) {
        return PATRON_PRINC_PATTERN.matcher(principal).matches();
    }

    /**
     * Given a set of principals, classifies each principal by whether it is
     * identifiably a patron or staff principal, adding the principals to
     * patronPrincipals or staffPrincipals accordingly.
     *
     * @param principals
     * @param patronPrincipals
     * @param staffPrincipals
     */
    public static void classifyPrincipals(Set<String> principals, Set<String> patronPrincipals,
            Set<String> staffPrincipals) {
        principals.forEach(agent -> {
            if (PATRON_PRINC_PATTERN.matcher(agent).matches()) {
                patronPrincipals.add(agent);
            } else {
                staffPrincipals.add(agent);
            }
        });
    }

    /**
     * Returns a set containing all patron principals found in the given set of
     * principals
     *
     * @param principals
     * @return
     */
    public static Set<String> getPatronPrincipals(Set<String> principals) {
        return principals.stream()
                .filter(agent -> PATRON_PRINC_PATTERN.matcher(agent).matches())
                .collect(Collectors.toSet());
    }
}
