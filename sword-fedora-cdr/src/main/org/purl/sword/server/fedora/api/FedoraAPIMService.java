/**
 * FedoraAPIMService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public interface FedoraAPIMService extends javax.xml.rpc.Service {
    public java.lang.String getmanagementAddress();

    public org.purl.sword.server.fedora.api.FedoraAPIM getmanagement() throws javax.xml.rpc.ServiceException;

    public org.purl.sword.server.fedora.api.FedoraAPIM getmanagement(java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
