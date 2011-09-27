/**
 * FedoraAPIM.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public interface FedoraAPIM extends java.rmi.Remote {
    public java.lang.String ingest(byte[] objectXML, java.lang.String format, java.lang.String logMessage) throws java.rmi.RemoteException;
    public java.lang.String ingestObject(byte[] METSXML, java.lang.String logMessage) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.UserInfo describeUser(java.lang.String id) throws java.rmi.RemoteException;
    public java.lang.String modifyObject(java.lang.String pid, java.lang.String state, java.lang.String label, java.lang.String ownerId, java.lang.String logMessage) throws java.rmi.RemoteException;
    public byte[] getObjectXML(java.lang.String pid) throws java.rmi.RemoteException;
    public byte[] export(java.lang.String pid, java.lang.String format, java.lang.String context) throws java.rmi.RemoteException;
    public byte[] exportObject(java.lang.String pid) throws java.rmi.RemoteException;
    public java.lang.String purgeObject(java.lang.String pid, java.lang.String logMessage, boolean force) throws java.rmi.RemoteException;
    public java.lang.String addDatastream(java.lang.String pid, java.lang.String dsID, java.lang.String[] altIDs, java.lang.String dsLabel, boolean versionable, java.lang.String MIMEType, java.lang.String formatURI, java.lang.String dsLocation, java.lang.String controlGroup, java.lang.String dsState, java.lang.String checksumType, java.lang.String checksum, java.lang.String logMessage) throws java.rmi.RemoteException;
    public java.lang.String addDisseminator(java.lang.String pid, java.lang.String bDefPID, java.lang.String bMechPID, java.lang.String dissLabel, org.purl.sword.server.fedora.api.DatastreamBindingMap bindingMap, java.lang.String dissState, java.lang.String logMessage) throws java.rmi.RemoteException;
    public java.lang.String modifyDatastreamByReference(java.lang.String pid, java.lang.String dsID, java.lang.String[] altIDs, java.lang.String dsLabel, java.lang.String MIMEType, java.lang.String formatURI, java.lang.String dsLocation, java.lang.String checksumType, java.lang.String checksum, java.lang.String logMessage, boolean force) throws java.rmi.RemoteException;
    public java.lang.String modifyDatastreamByValue(java.lang.String pid, java.lang.String dsID, java.lang.String[] altIDs, java.lang.String dsLabel, java.lang.String MIMEType, java.lang.String formatURI, byte[] dsContent, java.lang.String checksumType, java.lang.String checksum, java.lang.String logMessage, boolean force) throws java.rmi.RemoteException;
    public java.lang.String modifyDisseminator(java.lang.String pid, java.lang.String dissID, java.lang.String bMechPID, java.lang.String dissLabel, org.purl.sword.server.fedora.api.DatastreamBindingMap bindingMap, java.lang.String dissState, java.lang.String logMessage, boolean force) throws java.rmi.RemoteException;
    public java.lang.String setDatastreamState(java.lang.String pid, java.lang.String dsID, java.lang.String dsState, java.lang.String logMessage) throws java.rmi.RemoteException;
    public java.lang.String setDatastreamVersionable(java.lang.String pid, java.lang.String dsID, boolean versionable, java.lang.String logMessage) throws java.rmi.RemoteException;
    public java.lang.String compareDatastreamChecksum(java.lang.String pid, java.lang.String dsID, java.lang.String versionDate) throws java.rmi.RemoteException;
    public java.lang.String setDisseminatorState(java.lang.String pid, java.lang.String dissID, java.lang.String dissState, java.lang.String logMessage) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.Datastream getDatastream(java.lang.String pid, java.lang.String dsID, java.lang.String asOfDateTime) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.Datastream[] getDatastreams(java.lang.String pid, java.lang.String asOfDateTime, java.lang.String dsState) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.Datastream[] getDatastreamHistory(java.lang.String pid, java.lang.String dsID) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.Disseminator getDisseminator(java.lang.String pid, java.lang.String dissID, java.lang.String asOfDateTime) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.Disseminator[] getDisseminators(java.lang.String pid, java.lang.String asOfDateTime, java.lang.String dissState) throws java.rmi.RemoteException;
    public org.purl.sword.server.fedora.api.Disseminator[] getDisseminatorHistory(java.lang.String pid, java.lang.String dissID) throws java.rmi.RemoteException;
    public java.lang.String[] purgeDatastream(java.lang.String pid, java.lang.String dsID, java.lang.String startDT, java.lang.String endDT, java.lang.String logMessage, boolean force) throws java.rmi.RemoteException;
    public java.lang.String[] purgeDisseminator(java.lang.String pid, java.lang.String dissID, java.lang.String endDT, java.lang.String logMessage) throws java.rmi.RemoteException;
    public java.lang.String[] getNextPID(org.apache.axis.types.NonNegativeInteger numPIDs, java.lang.String pidNamespace) throws java.rmi.RemoteException;
}
