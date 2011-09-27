/**
 * FedoraAPIA.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public interface FedoraAPIA extends java.rmi.Remote {
    public org.purl.sword.server.fedora.api.RepositoryInfo describeRepository() throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.ObjectProfile getObjectProfile(java.lang.String pid, java.lang.String asOfDateTime) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.ObjectMethodsDef[] listMethods(java.lang.String pid, java.lang.String asOfDateTime) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.DatastreamDef[] listDatastreams(java.lang.String pid, java.lang.String asOfDateTime) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.MIMETypedStream getDatastreamDissemination(java.lang.String pid, java.lang.String dsID, java.lang.String asOfDateTime) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.MIMETypedStream getDissemination(java.lang.String pid, java.lang.String bDefPid, java.lang.String methodName, org.purl.sword.server.fedora.api.Property[] parameters, java.lang.String asOfDateTime) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.FieldSearchResult findObjects(java.lang.String[] resultFields, org.apache.axis.types.NonNegativeInteger maxResults, org.purl.sword.server.fedora.api.FieldSearchQuery query) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.FieldSearchResult resumeFindObjects(java.lang.String sessionToken) throws java.rmi.RemoteException;
    public java.lang.String[] getObjectHistory(java.lang.String pid) throws java.rmi.RemoteException;
}
