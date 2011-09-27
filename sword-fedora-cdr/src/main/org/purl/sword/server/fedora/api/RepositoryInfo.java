/**
 * RepositoryInfo.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class RepositoryInfo  implements java.io.Serializable {
    private java.lang.String repositoryName;

    private java.lang.String repositoryVersion;

    private java.lang.String repositoryBaseURL;

    private java.lang.String repositoryPIDNamespace;

    private java.lang.String defaultExportFormat;

    private java.lang.String OAINamespace;

    private java.lang.String[] adminEmailList;

    private java.lang.String samplePID;

    private java.lang.String sampleOAIIdentifier;

    private java.lang.String sampleSearchURL;

    private java.lang.String sampleAccessURL;

    private java.lang.String sampleOAIURL;

    private java.lang.String[] retainPIDs;

    public RepositoryInfo() {
    }

    public RepositoryInfo(
           java.lang.String repositoryName,
           java.lang.String repositoryVersion,
           java.lang.String repositoryBaseURL,
           java.lang.String repositoryPIDNamespace,
           java.lang.String defaultExportFormat,
           java.lang.String OAINamespace,
           java.lang.String[] adminEmailList,
           java.lang.String samplePID,
           java.lang.String sampleOAIIdentifier,
           java.lang.String sampleSearchURL,
           java.lang.String sampleAccessURL,
           java.lang.String sampleOAIURL,
           java.lang.String[] retainPIDs) {
           this.repositoryName = repositoryName;
           this.repositoryVersion = repositoryVersion;
           this.repositoryBaseURL = repositoryBaseURL;
           this.repositoryPIDNamespace = repositoryPIDNamespace;
           this.defaultExportFormat = defaultExportFormat;
           this.OAINamespace = OAINamespace;
           this.adminEmailList = adminEmailList;
           this.samplePID = samplePID;
           this.sampleOAIIdentifier = sampleOAIIdentifier;
           this.sampleSearchURL = sampleSearchURL;
           this.sampleAccessURL = sampleAccessURL;
           this.sampleOAIURL = sampleOAIURL;
           this.retainPIDs = retainPIDs;
    }


    /**
     * Gets the repositoryName value for this RepositoryInfo.
     * 
     * @return repositoryName
     */
    public java.lang.String getRepositoryName() {
        return repositoryName;
    }


    /**
     * Sets the repositoryName value for this RepositoryInfo.
     * 
     * @param repositoryName
     */
    public void setRepositoryName(java.lang.String repositoryName) {
        this.repositoryName = repositoryName;
    }


    /**
     * Gets the repositoryVersion value for this RepositoryInfo.
     * 
     * @return repositoryVersion
     */
    public java.lang.String getRepositoryVersion() {
        return repositoryVersion;
    }


    /**
     * Sets the repositoryVersion value for this RepositoryInfo.
     * 
     * @param repositoryVersion
     */
    public void setRepositoryVersion(java.lang.String repositoryVersion) {
        this.repositoryVersion = repositoryVersion;
    }


    /**
     * Gets the repositoryBaseURL value for this RepositoryInfo.
     * 
     * @return repositoryBaseURL
     */
    public java.lang.String getRepositoryBaseURL() {
        return repositoryBaseURL;
    }


    /**
     * Sets the repositoryBaseURL value for this RepositoryInfo.
     * 
     * @param repositoryBaseURL
     */
    public void setRepositoryBaseURL(java.lang.String repositoryBaseURL) {
        this.repositoryBaseURL = repositoryBaseURL;
    }


    /**
     * Gets the repositoryPIDNamespace value for this RepositoryInfo.
     * 
     * @return repositoryPIDNamespace
     */
    public java.lang.String getRepositoryPIDNamespace() {
        return repositoryPIDNamespace;
    }


    /**
     * Sets the repositoryPIDNamespace value for this RepositoryInfo.
     * 
     * @param repositoryPIDNamespace
     */
    public void setRepositoryPIDNamespace(java.lang.String repositoryPIDNamespace) {
        this.repositoryPIDNamespace = repositoryPIDNamespace;
    }


    /**
     * Gets the defaultExportFormat value for this RepositoryInfo.
     * 
     * @return defaultExportFormat
     */
    public java.lang.String getDefaultExportFormat() {
        return defaultExportFormat;
    }


    /**
     * Sets the defaultExportFormat value for this RepositoryInfo.
     * 
     * @param defaultExportFormat
     */
    public void setDefaultExportFormat(java.lang.String defaultExportFormat) {
        this.defaultExportFormat = defaultExportFormat;
    }


    /**
     * Gets the OAINamespace value for this RepositoryInfo.
     * 
     * @return OAINamespace
     */
    public java.lang.String getOAINamespace() {
        return OAINamespace;
    }


    /**
     * Sets the OAINamespace value for this RepositoryInfo.
     * 
     * @param OAINamespace
     */
    public void setOAINamespace(java.lang.String OAINamespace) {
        this.OAINamespace = OAINamespace;
    }


    /**
     * Gets the adminEmailList value for this RepositoryInfo.
     * 
     * @return adminEmailList
     */
    public java.lang.String[] getAdminEmailList() {
        return adminEmailList;
    }


    /**
     * Sets the adminEmailList value for this RepositoryInfo.
     * 
     * @param adminEmailList
     */
    public void setAdminEmailList(java.lang.String[] adminEmailList) {
        this.adminEmailList = adminEmailList;
    }


    /**
     * Gets the samplePID value for this RepositoryInfo.
     * 
     * @return samplePID
     */
    public java.lang.String getSamplePID() {
        return samplePID;
    }


    /**
     * Sets the samplePID value for this RepositoryInfo.
     * 
     * @param samplePID
     */
    public void setSamplePID(java.lang.String samplePID) {
        this.samplePID = samplePID;
    }


    /**
     * Gets the sampleOAIIdentifier value for this RepositoryInfo.
     * 
     * @return sampleOAIIdentifier
     */
    public java.lang.String getSampleOAIIdentifier() {
        return sampleOAIIdentifier;
    }


    /**
     * Sets the sampleOAIIdentifier value for this RepositoryInfo.
     * 
     * @param sampleOAIIdentifier
     */
    public void setSampleOAIIdentifier(java.lang.String sampleOAIIdentifier) {
        this.sampleOAIIdentifier = sampleOAIIdentifier;
    }


    /**
     * Gets the sampleSearchURL value for this RepositoryInfo.
     * 
     * @return sampleSearchURL
     */
    public java.lang.String getSampleSearchURL() {
        return sampleSearchURL;
    }


    /**
     * Sets the sampleSearchURL value for this RepositoryInfo.
     * 
     * @param sampleSearchURL
     */
    public void setSampleSearchURL(java.lang.String sampleSearchURL) {
        this.sampleSearchURL = sampleSearchURL;
    }


    /**
     * Gets the sampleAccessURL value for this RepositoryInfo.
     * 
     * @return sampleAccessURL
     */
    public java.lang.String getSampleAccessURL() {
        return sampleAccessURL;
    }


    /**
     * Sets the sampleAccessURL value for this RepositoryInfo.
     * 
     * @param sampleAccessURL
     */
    public void setSampleAccessURL(java.lang.String sampleAccessURL) {
        this.sampleAccessURL = sampleAccessURL;
    }


    /**
     * Gets the sampleOAIURL value for this RepositoryInfo.
     * 
     * @return sampleOAIURL
     */
    public java.lang.String getSampleOAIURL() {
        return sampleOAIURL;
    }


    /**
     * Sets the sampleOAIURL value for this RepositoryInfo.
     * 
     * @param sampleOAIURL
     */
    public void setSampleOAIURL(java.lang.String sampleOAIURL) {
        this.sampleOAIURL = sampleOAIURL;
    }


    /**
     * Gets the retainPIDs value for this RepositoryInfo.
     * 
     * @return retainPIDs
     */
    public java.lang.String[] getRetainPIDs() {
        return retainPIDs;
    }


    /**
     * Sets the retainPIDs value for this RepositoryInfo.
     * 
     * @param retainPIDs
     */
    public void setRetainPIDs(java.lang.String[] retainPIDs) {
        this.retainPIDs = retainPIDs;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof RepositoryInfo)) return false;
        RepositoryInfo other = (RepositoryInfo) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.repositoryName==null && other.getRepositoryName()==null) || 
             (this.repositoryName!=null &&
              this.repositoryName.equals(other.getRepositoryName()))) &&
            ((this.repositoryVersion==null && other.getRepositoryVersion()==null) || 
             (this.repositoryVersion!=null &&
              this.repositoryVersion.equals(other.getRepositoryVersion()))) &&
            ((this.repositoryBaseURL==null && other.getRepositoryBaseURL()==null) || 
             (this.repositoryBaseURL!=null &&
              this.repositoryBaseURL.equals(other.getRepositoryBaseURL()))) &&
            ((this.repositoryPIDNamespace==null && other.getRepositoryPIDNamespace()==null) || 
             (this.repositoryPIDNamespace!=null &&
              this.repositoryPIDNamespace.equals(other.getRepositoryPIDNamespace()))) &&
            ((this.defaultExportFormat==null && other.getDefaultExportFormat()==null) || 
             (this.defaultExportFormat!=null &&
              this.defaultExportFormat.equals(other.getDefaultExportFormat()))) &&
            ((this.OAINamespace==null && other.getOAINamespace()==null) || 
             (this.OAINamespace!=null &&
              this.OAINamespace.equals(other.getOAINamespace()))) &&
            ((this.adminEmailList==null && other.getAdminEmailList()==null) || 
             (this.adminEmailList!=null &&
              java.util.Arrays.equals(this.adminEmailList, other.getAdminEmailList()))) &&
            ((this.samplePID==null && other.getSamplePID()==null) || 
             (this.samplePID!=null &&
              this.samplePID.equals(other.getSamplePID()))) &&
            ((this.sampleOAIIdentifier==null && other.getSampleOAIIdentifier()==null) || 
             (this.sampleOAIIdentifier!=null &&
              this.sampleOAIIdentifier.equals(other.getSampleOAIIdentifier()))) &&
            ((this.sampleSearchURL==null && other.getSampleSearchURL()==null) || 
             (this.sampleSearchURL!=null &&
              this.sampleSearchURL.equals(other.getSampleSearchURL()))) &&
            ((this.sampleAccessURL==null && other.getSampleAccessURL()==null) || 
             (this.sampleAccessURL!=null &&
              this.sampleAccessURL.equals(other.getSampleAccessURL()))) &&
            ((this.sampleOAIURL==null && other.getSampleOAIURL()==null) || 
             (this.sampleOAIURL!=null &&
              this.sampleOAIURL.equals(other.getSampleOAIURL()))) &&
            ((this.retainPIDs==null && other.getRetainPIDs()==null) || 
             (this.retainPIDs!=null &&
              java.util.Arrays.equals(this.retainPIDs, other.getRetainPIDs())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getRepositoryName() != null) {
            _hashCode += getRepositoryName().hashCode();
        }
        if (getRepositoryVersion() != null) {
            _hashCode += getRepositoryVersion().hashCode();
        }
        if (getRepositoryBaseURL() != null) {
            _hashCode += getRepositoryBaseURL().hashCode();
        }
        if (getRepositoryPIDNamespace() != null) {
            _hashCode += getRepositoryPIDNamespace().hashCode();
        }
        if (getDefaultExportFormat() != null) {
            _hashCode += getDefaultExportFormat().hashCode();
        }
        if (getOAINamespace() != null) {
            _hashCode += getOAINamespace().hashCode();
        }
        if (getAdminEmailList() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getAdminEmailList());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getAdminEmailList(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getSamplePID() != null) {
            _hashCode += getSamplePID().hashCode();
        }
        if (getSampleOAIIdentifier() != null) {
            _hashCode += getSampleOAIIdentifier().hashCode();
        }
        if (getSampleSearchURL() != null) {
            _hashCode += getSampleSearchURL().hashCode();
        }
        if (getSampleAccessURL() != null) {
            _hashCode += getSampleAccessURL().hashCode();
        }
        if (getSampleOAIURL() != null) {
            _hashCode += getSampleOAIURL().hashCode();
        }
        if (getRetainPIDs() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getRetainPIDs());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getRetainPIDs(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(RepositoryInfo.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "RepositoryInfo"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("repositoryName");
        elemField.setXmlName(new javax.xml.namespace.QName("", "repositoryName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("repositoryVersion");
        elemField.setXmlName(new javax.xml.namespace.QName("", "repositoryVersion"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("repositoryBaseURL");
        elemField.setXmlName(new javax.xml.namespace.QName("", "repositoryBaseURL"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("repositoryPIDNamespace");
        elemField.setXmlName(new javax.xml.namespace.QName("", "repositoryPIDNamespace"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("defaultExportFormat");
        elemField.setXmlName(new javax.xml.namespace.QName("", "defaultExportFormat"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("OAINamespace");
        elemField.setXmlName(new javax.xml.namespace.QName("", "OAINamespace"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("adminEmailList");
        elemField.setXmlName(new javax.xml.namespace.QName("", "adminEmailList"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/api/", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("samplePID");
        elemField.setXmlName(new javax.xml.namespace.QName("", "samplePID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sampleOAIIdentifier");
        elemField.setXmlName(new javax.xml.namespace.QName("", "sampleOAIIdentifier"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sampleSearchURL");
        elemField.setXmlName(new javax.xml.namespace.QName("", "sampleSearchURL"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sampleAccessURL");
        elemField.setXmlName(new javax.xml.namespace.QName("", "sampleAccessURL"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sampleOAIURL");
        elemField.setXmlName(new javax.xml.namespace.QName("", "sampleOAIURL"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("retainPIDs");
        elemField.setXmlName(new javax.xml.namespace.QName("", "retainPIDs"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/api/", "item"));
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
