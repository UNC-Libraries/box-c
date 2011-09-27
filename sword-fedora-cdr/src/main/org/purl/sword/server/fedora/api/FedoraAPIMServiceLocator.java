/**
 * FedoraAPIMServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class FedoraAPIMServiceLocator extends org.apache.axis.client.Service implements org.purl.sword.server.fedora.api.FedoraAPIMService {

    public FedoraAPIMServiceLocator() {
    }


    public FedoraAPIMServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public FedoraAPIMServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for management
    private java.lang.String management_address = "http://localhost:8080/fedora/services/management";

    public java.lang.String getmanagementAddress() {
        return management_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String managementWSDDServiceName = "management";

    public java.lang.String getmanagementWSDDServiceName() {
        return managementWSDDServiceName;
    }

    public void setmanagementWSDDServiceName(java.lang.String name) {
        managementWSDDServiceName = name;
    }

    public org.purl.sword.server.fedora.api.FedoraAPIM getmanagement() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(management_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getmanagement(endpoint);
    }

    public org.purl.sword.server.fedora.api.FedoraAPIM getmanagement(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            org.purl.sword.server.fedora.api.ManagementSoapBindingStub _stub = new org.purl.sword.server.fedora.api.ManagementSoapBindingStub(portAddress, this);
            _stub.setPortName(getmanagementWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setmanagementEndpointAddress(java.lang.String address) {
        management_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (org.purl.sword.server.fedora.api.FedoraAPIM.class.isAssignableFrom(serviceEndpointInterface)) {
                org.purl.sword.server.fedora.api.ManagementSoapBindingStub _stub = new org.purl.sword.server.fedora.api.ManagementSoapBindingStub(new java.net.URL(management_address), this);
                _stub.setPortName(getmanagementWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("management".equals(inputPortName)) {
            return getmanagement();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/api/", "Fedora-API-M-Service");
    }

    private java.util.HashSet ports = null;

@SuppressWarnings(value={"unchecked"})
    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/api/", "management"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("management".equals(portName)) {
            setmanagementEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
