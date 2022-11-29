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
