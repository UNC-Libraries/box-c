package edu.unc.lib.dl.fcrepo4;

import java.net.URI;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.URIUtil;

public class RepositoryPaths {

    private String policiesBase;
    private String vocabulariesBase;
    private String depositRecordBase;
    private String contentBase;
    private String agentsBase;
    private String serverUri;
    private String baseUri;
    private String baseHost;

    protected URI getMetadataUri(PID pid) {
        String path = pid.getRepositoryPath();
        if (!path.endsWith(RepositoryPathConstants.FCR_METADATA)) {
            return URI.create(URIUtil.join(path,
                    RepositoryPathConstants.FCR_METADATA));
        } else {
            return pid.getRepositoryUri();
        }
    }

    public String getServerUri() {
        return serverUri;
    }

    public String getBaseUri() {
        return baseUri;
    }

    public String getBaseHost() {
        return baseHost;
    }

    public String getContentBase() {
        return contentBase;
    }

    public String getDepositRecordBase() {
        return depositRecordBase;
    }

    public String getAgentsBase() {
        return agentsBase;
    }

    public String getPoliciesBase() {
        return policiesBase;
    }

    public String getVocabulariesBase() {
        return vocabulariesBase;
    }

    public void setDepositRecordBase(String depositRecordBase) {
        this.depositRecordBase = depositRecordBase;
    }

    public void setVocabulariesBase(String vocabulariesBase) {
        this.vocabulariesBase = vocabulariesBase;
    }

    public void setContentBase(String contentBase) {
        this.contentBase = contentBase;
    }

    public void setAgentsBase(String agentsBase) {
        this.agentsBase = agentsBase;
    }

    public void setPoliciesBase(String policiesBase) {
        this.policiesBase = policiesBase;
    }

    public void setServerUri(String serverUri) {
        this.serverUri = serverUri;
        if (!serverUri.endsWith("/")) {
            this.serverUri += "/";
        }
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
        if (!baseUri.endsWith("/")) {
            this.baseUri += "/";
        }
    }

    public void setBaseHost(String baseHost) {
        this.baseHost = baseHost;
    }

}
