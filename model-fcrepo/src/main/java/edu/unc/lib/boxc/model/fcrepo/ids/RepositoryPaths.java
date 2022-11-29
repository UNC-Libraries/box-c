package edu.unc.lib.boxc.model.fcrepo.ids;

import static edu.unc.lib.boxc.model.api.ids.PIDConstants.DEPOSITS_QUALIFIER;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_BASE;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_ROOT_ID;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.DEPOSIT_RECORD_BASE;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.METADATA_CONTAINER;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.REPOSITORY_ROOT_ID;

import java.net.URI;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.FcrepoPaths;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;

/**
 * Provides access to base URIs needed for interacting with the repository
 *
 * @author harring
 *
 */
public class RepositoryPaths {

    private static String policiesBase;
    private static String vocabulariesBase;
    private static String depositRecordBase;
    private static PID depositRecordRootPid;
    private static String contentBase;
    private static String agentsBase;
    private static String serverUri;
    private static PID contentRootPid;
    private static PID contentBasePid;
    private static PID rootPid;

    static {
        setContentBase(FcrepoPaths.getBaseUri());
    }

    private RepositoryPaths() {

    }

    public static URI getMetadataUri(PID pid) {
        String path = pid.getRepositoryPath();
        if (!path.endsWith(RepositoryPathConstants.FCR_METADATA)) {
            return URI.create(URIUtil.join(path,
                    RepositoryPathConstants.FCR_METADATA));
        } else {
            return pid.getRepositoryUri();
        }
    }

    /**
     * Generate URI for the metadata container for the target object
     *
     * @param pid
     * @return
     */
    public static URI getMetadataContainerUri(PID pid) {
        return URI.create(URIUtil.join(pid.getRepositoryPath(), METADATA_CONTAINER));
    }

    public static String getServerUri() {
        return serverUri;
    }

    /**
     * @return the rootPid
     */
    public static PID getRootPid() {
        return rootPid;
    }

    /**
     * @return pid for the resource where content objects are stored.
     */
    public static PID getContentBasePid() {
        return contentBasePid;
    }

    /**
     * @return base uri for content objects
     */
    public static String getContentBase() {
        return contentBase;
    }

    /**
     * @return PID of the root object of the content tree.
     */
    public static PID getContentRootPid() {
        return contentRootPid;
    }

    /**
     * @return base uri for deposit record objects
     */
    public static String getDepositRecordBase() {
        return depositRecordBase;
    }

    /**
     * @return PID of the root object for the deposit tree
     */
    public static PID getDepositRecordRootPid() {
        return depositRecordRootPid;
    }

    public static String getAgentsBase() {
        return agentsBase;
    }

    public static String getPoliciesBase() {
        return policiesBase;
    }

    public static String getVocabulariesBase() {
        return vocabulariesBase;
    }

    /**
     * Returns hashed container container path for id,
     * Prepend id with defined levels of hashed containers based on the values.
     * For example, 9bd8b60e-93a2-4b66-8f0a-b62338483b39 would return
     *    9b/d8/b6
     *
     * @param id
     * @return
     */
    /**
     * Returns hashed container path for id.
     *
     * For example, 9bd8b60e-93a2-4b66-8f0a-b62338483b39 with pathDepth = 3 and chunkLength = 2 returns
     *    9b/d8/b6/
     *
     * @param id id to generate hashed path for.
     * @param pathDepth number of hashed levels to add to the path
     * @param hashLength number of characters per level
     * @return hash container path for id
     */
    public static String idToPath(String id, int pathDepth, int hashLength) {
        StringBuilder sb = new StringBuilder();

        // Expand the id into chunked subfolders
        for (int i = 0; i < pathDepth; i++) {
            sb.append(id.substring(i * hashLength, i * hashLength + hashLength))
                    .append('/');
        }

        return sb.toString();
    }

    /**
     * Initializes paths from repository base uri
     *
     * @param uri base uri for repository
     */
    private static void setContentBase(String fcrepoUri) {
        rootPid = new FedoraPID(REPOSITORY_ROOT_ID, REPOSITORY_ROOT_ID, null, URI.create(fcrepoUri));
        contentBase = URIUtil.join(fcrepoUri, CONTENT_BASE);
        contentBasePid = new FedoraPID(CONTENT_BASE, REPOSITORY_ROOT_ID, null, URI.create(contentBase));
        contentRootPid = PIDs.get(URIUtil.join(contentBase, CONTENT_ROOT_ID));
        depositRecordBase = URIUtil.join(fcrepoUri, DEPOSIT_RECORD_BASE);
        depositRecordRootPid = new FedoraPID(DEPOSITS_QUALIFIER, REPOSITORY_ROOT_ID, null,
                URI.create(depositRecordBase));
    }
}
