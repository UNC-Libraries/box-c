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
