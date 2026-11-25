package edu.unc.lib.boxc.tools.amq;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.StoragePolicy;

/**
 * Finds Longleaf-registerable binaries by recursively following Fedora containment relations.
 *
 * @author bbpennel
 */
public class FedoraContainmentWalker {
    private static final Property LDP_CONTAINS = ResourceFactory
            .createProperty("http://www.w3.org/ns/ldp#contains");
    private static final Resource LDP_NON_RDF_SOURCE = ResourceFactory
            .createResource("http://www.w3.org/ns/ldp#NonRDFSource");
    private static final Pattern NON_RDF_SOURCE_LINK = Pattern.compile("<"
            + Pattern.quote(LDP_NON_RDF_SOURCE.getURI()) + ">.*",
            Pattern.CASE_INSENSITIVE);

    private final HttpClient client;
    private final String fcrepoRequestBaseUrl;
    private final String fcrepoBaseUri;
    private final String username;
    private final String password;

    public FedoraContainmentWalker(String fcrepoRequestBaseUrl, String username, String password) {
        this(HttpClient.newHttpClient(), fcrepoRequestBaseUrl, fcrepoRequestBaseUrl, username, password);
    }

    FedoraContainmentWalker(HttpClient client, String fcrepoRequestBaseUrl, String fcrepoBaseUri, String username,
            String password) {
        this.client = client;
        this.fcrepoRequestBaseUrl = stripTrailingSlash(fcrepoRequestBaseUrl);
        this.fcrepoBaseUri = stripTrailingSlash(fcrepoBaseUri);
        this.username = username;
        this.password = password;
    }

    public List<String> findRegisterableBinaries(List<String> inputs) throws IOException, InterruptedException {
        Set<String> binaries = new LinkedHashSet<>();
        Set<URI> visited = new LinkedHashSet<>();
        ArrayDeque<URI> pending = new ArrayDeque<>();
        for (String input : inputs) {
            pending.add(toFedoraUri(input));
        }

        while (!pending.isEmpty()) {
            URI uri = pending.removeFirst();
            if (!visited.add(uri)) {
                continue;
            }
            if (isNonRdfSource(uri)) {
                if (isRegisterable(uri)) {
                    binaries.add(uri.toString());
                }
                continue;
            }
            Model model = getModel(uri);
            Resource resource = getResourceForUri(model, uri);
            if (resource.hasProperty(RDF.type, LDP_NON_RDF_SOURCE) && isRegisterable(uri)) {
                binaries.add(uri.toString());
            }
            model.listObjectsOfProperty(resource, LDP_CONTAINS)
                    .forEachRemaining(node -> addContainedUri(node, pending));
        }
        return List.copyOf(binaries);
    }

    private Model getModel(URI uri) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(toRequestUri(uri))
                .header("Accept", "text/turtle")
                .header("Prefer", "return=representation; include=\"http://www.w3.org/ns/ldp#PreferContainment\"")
                .GET();
        addCredentials(request);

        HttpResponse<byte[]> response = client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Fedora returned HTTP " + response.statusCode() + " for " + uri);
        }
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, new java.io.ByteArrayInputStream(response.body()), Lang.TURTLE);
        return model;
    }

    private boolean isNonRdfSource(URI uri) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(toRequestUri(uri))
                .method("HEAD", HttpRequest.BodyPublishers.noBody());
        addCredentials(request);

        HttpResponse<Void> response = client.send(request.build(), HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Fedora returned HTTP " + response.statusCode() + " for " + uri);
        }
        return response.headers().allValues("Link").stream()
                .anyMatch(link -> NON_RDF_SOURCE_LINK.matcher(link).find());
    }

    private void addCredentials(HttpRequest.Builder request) {
        if (username != null) {
            String credentials = username + ":" + (password == null ? "" : password);
            request.header("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void addContainedUri(RDFNode node, ArrayDeque<URI> pending) {
        if (node.isURIResource()) {
            pending.add(toCanonicalUri(URI.create(node.asResource().getURI())));
        }
    }

    private Resource getResourceForUri(Model model, URI uri) {
        Resource exactMatch = model.getResource(uri.toString());
        if (exactMatch.listProperties().hasNext()) {
            return exactMatch;
        }
        return model.listSubjects().toList().stream()
                .filter(Resource::isURIResource)
                .filter(resource -> toCanonicalUri(URI.create(resource.getURI())).equals(uri))
                .findFirst()
                .orElse(exactMatch);
    }

    /**
     * Converts a UUID input into its Fedora resource URI. For example, with
     * {@code fcrepoBaseUri=https://example.org/rest}, input
     * {@code 12345678-1234-1234-1234-123456789abc} becomes
     * {@code https://example.org/rest/content/12/34/56/78/12345678-1234-1234-1234-123456789abc}.
     * Input {@code deposits/12345678-1234-1234-1234-123456789abc/file} retains its qualifier and component path,
     * producing {@code https://example.org/rest/deposits/12/34/56/78/12345678-1234-1234-1234-123456789abc/file}.
     */
    private URI toFedoraUri(String input) {
        URI uri = URI.create(input);
        if (uri.isAbsolute()) {
            return toCanonicalUri(uri);
        }
        String qualifier = "content";
        String idAndComponent = input;
        if (input.startsWith("content/") || input.startsWith("deposits/")) {
            int separator = input.indexOf('/');
            qualifier = input.substring(0, separator);
            idAndComponent = input.substring(separator + 1);
        }
        if (idAndComponent.startsWith("uuid:")) {
            idAndComponent = idAndComponent.substring("uuid:".length());
        }

        int componentSeparator = idAndComponent.indexOf('/');
        String id = UUID.fromString(componentSeparator == -1 ? idAndComponent
                : idAndComponent.substring(0, componentSeparator)).toString();
        String componentPath = componentSeparator == -1 ? "" : idAndComponent.substring(componentSeparator);
        return URI.create(fcrepoBaseUri + "/" + qualifier + "/" + id.substring(0, 2) + "/"
                + id.substring(2, 4) + "/" + id.substring(4, 6) + "/" + id.substring(6, 8) + "/" + id
                + componentPath);
    }

    private URI toCanonicalUri(URI uri) {
        URI mappedUri = remapBase(uri, fcrepoRequestBaseUrl, fcrepoBaseUri);
        return mappedUri.equals(uri) ? remapBase(uri, fcrepoBaseUri, fcrepoBaseUri) : mappedUri;
    }

    private URI toRequestUri(URI uri) {
        return remapBase(uri, fcrepoBaseUri, fcrepoRequestBaseUrl);
    }

    private URI remapBase(URI uri, String sourceBase, String targetBase) {
        URI source = URI.create(sourceBase);
        String sourcePath = source.getRawPath();
        String resourcePath = uri.getRawPath();
        if (resourcePath != null && (resourcePath.equals(sourcePath)
                || resourcePath.startsWith(sourcePath + "/"))) {
            String suffix = resourcePath.substring(sourcePath.length());
            return URI.create(targetBase + suffix);
        }
        return uri;
    }

    private boolean isRegisterable(URI uri) {
        String path = uri.getPath();
        int separator = path.lastIndexOf('/');
        String name = separator == -1 ? path : path.substring(separator + 1);
        DatastreamType datastreamType = DatastreamType.getByIdentifier(name);
        return (datastreamType != null && datastreamType.getStoragePolicy() == StoragePolicy.INTERNAL)
                || path.contains("/deposits/");
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
