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
package edu.unc.lib.dl.fcrepo4;

import static edu.unc.lib.dl.util.RDFModelUtil.TURTLE_MIMETYPE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.jena.rdf.model.Property;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.dl.fedora.FedoraException;

/**
 * Factory for creating LDP container objects in the repository
 *
 * @author bbpennel
 *
 */
public class LdpContainerFactory {

    private FcrepoClient client;

    private static final String DIRECT_CONTAINER_TTL =
            "@prefix ldp: <http://www.w3.org/ns/ldp#>\n" +
            "<> a ldp:DirectContainer ;\n" +
            " ldp:membershipResource <.> ;\n" +
            " ldp:hasMemberRelation <%s> .";

    private static final String DIRECT_FILESET_TTL =
            "@prefix ldp: <http://www.w3.org/ns/ldp#>\n" +
            "@prefix pcdm: <http://pcdm.org/models#>\n" +
            "<> a ldp:DirectContainer ;\n" +
            " a pcdm:FileSet ;\n" +
            " ldp:membershipResource <.> ;\n" +
            " ldp:hasMemberRelation pcdm:hasFile .";

    private static final String INDIRECT_CONTAINER_TTL =
            "@prefix ldp: <http://www.w3.org/ns/ldp#>\n" +
            "@prefix ore: <http://www.openarchives.org/ore/terms/>\n" +
            "<> a ldp:IndirectContainer ;\n" +
            " ldp:membershipResource <.> ;\n" +
            " ldp:hasMemberRelation <%s> ;\n" +
            " ldp:insertedContentRelation ore:proxyFor .";

    private static final String INDIRECT_PROXY_TTL =
            "@prefix ore: <http://www.openarchives.org/ore/terms/>\n" +
            "<> ore:proxyFor <%s> ;\n" +
            " ore:proxyIn <%s> .";

    /**
     * Creates a LDP direct container as the child of the given membership
     * resource for the supplied member relation.
     *
     * @param membershipResource
     *            URI of the resource that will contain this direct container
     *            and receive the member relations
     * @param memberRelation
     *            relation that will be added for each child of this container
     * @param name
     *            suggested name for the direct container resource
     * @return the path of the newly created direct container
     * @throws FedoraException
     * @throws IOException
     */
    public URI createDirectContainer(URI membershipResource, Property memberRelation, String name)
            throws FedoraException, IOException {
        String relations = String.format(DIRECT_CONTAINER_TTL, memberRelation.toString());

        return createLdpContainer(membershipResource, relations, name);
    }

    /**
     * Creates a PCDM FileSet implemented as a LDP direct container
     *
     * @param membershipResource
     *            URI of the resource that will contain this direct container
     *            and receive pcdm:hasFile relations
     * @param name
     *            suggested name for the direct container resource
     * @returnthe path of the newly created direct container
     * @throws FedoraException
     * @throws IOException
     */
    public URI createDirectFileSet(URI membershipResource, String name) throws FedoraException, IOException {
        return createLdpContainer(membershipResource, DIRECT_FILESET_TTL, name);
    }

    /**
     * Creates an LDP indirect container as the child of the given membership
     * resource
     *
     * @param membershipResource
     *            URI of the resource that will contain this indirect container
     *            and receive the member relations
     * @param memberRelation
     *            relation that will be added for each child proxy of this
     *            container
     * @param name
     *            suggested name for the indirect container resource
     * @return the path of the newly created indirect container
     * @throws FedoraException
     * @throws IOException
     */
    public URI createIndirectContainer(URI membershipResource, Property memberRelation, String name)
            throws FedoraException, IOException {
        String relations = String.format(INDIRECT_CONTAINER_TTL, memberRelation.toString());

        return createLdpContainer(membershipResource, relations, name);
    }

    private URI createLdpContainer(URI membershipResource, String relationTtl, String name)
            throws FedoraException, IOException {
        try (FcrepoResponse response = client.post(membershipResource)
                .body(new ByteArrayInputStream(relationTtl.getBytes(StandardCharsets.UTF_8)), TURTLE_MIMETYPE)
                .slug(name)
                .perform()) {

            return response.getLocation();
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    /**
     * Creates a proxy for an object within an indirect container
     *
     * @param container the indirect container where the proxy will be created
     * @param proxyIn the URI of the resource which will be the subject of the generated relationship
     * @param proxyFor the URI of the resource which will be the object of the generated relationship
     * @return the URI of the created proxy
     * @throws FedoraException
     */
    public URI createIndirectProxy(URI container, URI proxyIn, URI proxyFor) throws FedoraException {
        String relations = String.format(INDIRECT_PROXY_TTL, proxyFor.toString(), proxyIn.toString());

        try (FcrepoResponse response = client.post(container)
                .body(new ByteArrayInputStream(relations.getBytes(StandardCharsets.UTF_8)), TURTLE_MIMETYPE)
                .perform()
                ) {
            return response.getLocation();
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        } catch (IOException e) {
            throw new FedoraException("Unable to close response stream", e);
        }
    }

    public FcrepoClient getClient() {
        return client;
    }

    public void setClient(FcrepoClient client) {
        this.client = client;
    }
}
