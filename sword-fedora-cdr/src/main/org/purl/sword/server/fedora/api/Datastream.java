/**
 * Datastream.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class Datastream  implements java.io.Serializable {
    private org.purl.sword.server.fedora.api.DatastreamControlGroup controlGroup;

    private java.lang.String ID;

    private java.lang.String versionID;

    private java.lang.String[] altIDs;

    private java.lang.String label;

    private boolean versionable;

    private java.lang.String MIMEType;

    private java.lang.String formatURI;

    private java.lang.String createDate;

    private long size;

    private java.lang.String state;

    private java.lang.String location;

    private java.lang.String checksumType;

    private java.lang.String checksum;

    public Datastream() {
    }

    public Datastream(
           org.purl.sword.server.fedora.api.DatastreamControlGroup controlGroup,
           java.lang.String ID,
           java.lang.String versionID,
           java.lang.String[] altIDs,
           java.lang.String label,
           boolean versionable,
           java.lang.String MIMEType,
           java.lang.String formatURI,
           java.lang.String createDate,
           long size,
           java.lang.String state,
           java.lang.String location,
           java.lang.String checksumType,
           java.lang.String checksum) {
           this.controlGroup = controlGroup;
           this.ID = ID;
           this.versionID = versionID;
           this.altIDs = altIDs;
           this.label = label;
           this.versionable = versionable;
           this.MIMEType = MIMEType;
           this.formatURI = formatURI;
           this.createDate = createDate;
           this.size = size;
           this.state = state;
           this.location = location;
           this.checksumType = checksumType;
           this.checksum = checksum;
    }


    /**
     * Gets the controlGroup value for this Datastream.
     * 
     * @return controlGroup
     */
    public org.purl.sword.server.fedora.api.DatastreamControlGroup getControlGroup() {
        return controlGroup;
    }


    /**
     * Sets the controlGroup value for this Datastream.
     * 
     * @param controlGroup
     */
    public void setControlGroup(org.purl.sword.server.fedora.api.DatastreamControlGroup controlGroup) {
        this.controlGroup = controlGroup;
    }


    /**
     * Gets the ID value for this Datastream.
     * 
     * @return ID
     */
    public java.lang.String getID() {
        return ID;
    }


    /**
     * Sets the ID value for this Datastream.
     * 
     * @param ID
     */
    public void setID(java.lang.String ID) {
        this.ID = ID;
    }


    /**
     * Gets the versionID value for this Datastream.
     * 
     * @return versionID
     */
    public java.lang.String getVersionID() {
        return versionID;
    }


    /**
     * Sets the versionID value for this Datastream.
     * 
     * @param versionID
     */
    public void setVersionID(java.lang.String versionID) {
        this.versionID = versionID;
    }


    /**
     * Gets the altIDs value for this Datastream.
     * 
     * @return altIDs
     */
    public java.lang.String[] getAltIDs() {
        return altIDs;
    }


    /**
     * Sets the altIDs value for this Datastream.
     * 
     * @param altIDs
     */
    public void setAltIDs(java.lang.String[] altIDs) {
        this.altIDs = altIDs;
    }


    /**
     * Gets the label value for this Datastream.
     * 
     * @return label
     */
    public java.lang.String getLabel() {
        return label;
    }


    /**
     * Sets the label value for this Datastream.
     * 
     * @param label
     */
    public void setLabel(java.lang.String label) {
        this.label = label;
    }


    /**
     * Gets the versionable value for this Datastream.
     * 
     * @return versionable
     */
    public boolean isVersionable() {
        return versionable;
    }


    /**
     * Sets the versionable value for this Datastream.
     * 
     * @param versionable
     */
    public void setVersionable(boolean versionable) {
        this.versionable = versionable;
    }


    /**
     * Gets the MIMEType value for this Datastream.
     * 
     * @return MIMEType
     */
    public java.lang.String getMIMEType() {
        return MIMEType;
    }


    /**
     * Sets the MIMEType value for this Datastream.
     * 
     * @param MIMEType
     */
    public void setMIMEType(java.lang.String MIMEType) {
        this.MIMEType = MIMEType;
    }


    /**
     * Gets the formatURI value for this Datastream.
     * 
     * @return formatURI
     */
    public java.lang.String getFormatURI() {
        return formatURI;
    }


    /**
     * Sets the formatURI value for this Datastream.
     * 
     * @param formatURI
     */
    public void setFormatURI(java.lang.String formatURI) {
        this.formatURI = formatURI;
    }


    /**
     * Gets the createDate value for this Datastream.
     * 
     * @return createDate
     */
    public java.lang.String getCreateDate() {
        return createDate;
    }


    /**
     * Sets the createDate value for this Datastream.
     * 
     * @param createDate
     */
    public void setCreateDate(java.lang.String createDate) {
        this.createDate = createDate;
    }


    /**
     * Gets the size value for this Datastream.
     * 
     * @return size
     */
    public long getSize() {
        return size;
    }


    /**
     * Sets the size value for this Datastream.
     * 
     * @param size
     */
    public void setSize(long size) {
        this.size = size;
    }


    /**
     * Gets the state value for this Datastream.
     * 
     * @return state
     */
    public java.lang.String getState() {
        return state;
    }


    /**
     * Sets the state value for this Datastream.
     * 
     * @param state
     */
    public void setState(java.lang.String state) {
        this.state = state;
    }


    /**
     * Gets the location value for this Datastream.
     * 
     * @return location
     */
    public java.lang.String getLocation() {
        return location;
    }


    /**
     * Sets the location value for this Datastream.
     * 
     * @param location
     */
    public void setLocation(java.lang.String location) {
        this.location = location;
    }


    /**
     * Gets the checksumType value for this Datastream.
     * 
     * @return checksumType
     */
    public java.lang.String getChecksumType() {
        return checksumType;
    }


    /**
     * Sets the checksumType value for this Datastream.
     * 
     * @param checksumType
     */
    public void setChecksumType(java.lang.String checksumType) {
        this.checksumType = checksumType;
    }


    /**
     * Gets the checksum value for this Datastream.
     * 
     * @return checksum
     */
    public java.lang.String getChecksum() {
        return checksum;
    }


    /**
     * Sets the checksum value for this Datastream.
     * 
     * @param checksum
     */
    public void setChecksum(java.lang.String checksum) {
        this.checksum = checksum;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Datastream)) return false;
        Datastream other = (Datastream) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.controlGroup==null && other.getControlGroup()==null) || 
             (this.controlGroup!=null &&
              this.controlGroup.equals(other.getControlGroup()))) &&
            ((this.ID==null && other.getID()==null) || 
             (this.ID!=null &&
              this.ID.equals(other.getID()))) &&
            ((this.versionID==null && other.getVersionID()==null) || 
             (this.versionID!=null &&
              this.versionID.equals(other.getVersionID()))) &&
            ((this.altIDs==null && other.getAltIDs()==null) || 
             (this.altIDs!=null &&
              java.util.Arrays.equals(this.altIDs, other.getAltIDs()))) &&
            ((this.label==null && other.getLabel()==null) || 
             (this.label!=null &&
              this.label.equals(other.getLabel()))) &&
            this.versionable == other.isVersionable() &&
            ((this.MIMEType==null && other.getMIMEType()==null) || 
             (this.MIMEType!=null &&
              this.MIMEType.equals(other.getMIMEType()))) &&
            ((this.formatURI==null && other.getFormatURI()==null) || 
             (this.formatURI!=null &&
              this.formatURI.equals(other.getFormatURI()))) &&
            ((this.createDate==null && other.getCreateDate()==null) || 
             (this.createDate!=null &&
              this.createDate.equals(other.getCreateDate()))) &&
            this.size == other.getSize() &&
            ((this.state==null && other.getState()==null) || 
             (this.state!=null &&
              this.state.equals(other.getState()))) &&
            ((this.location==null && other.getLocation()==null) || 
             (this.location!=null &&
              this.location.equals(other.getLocation()))) &&
            ((this.checksumType==null && other.getChecksumType()==null) || 
             (this.checksumType!=null &&
              this.checksumType.equals(other.getChecksumType()))) &&
            ((this.checksum==null && other.getChecksum()==null) || 
             (this.checksum!=null &&
              this.checksum.equals(other.getChecksum())));
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
        if (getControlGroup() != null) {
            _hashCode += getControlGroup().hashCode();
        }
        if (getID() != null) {
            _hashCode += getID().hashCode();
        }
        if (getVersionID() != null) {
            _hashCode += getVersionID().hashCode();
        }
        if (getAltIDs() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getAltIDs());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getAltIDs(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getLabel() != null) {
            _hashCode += getLabel().hashCode();
        }
        _hashCode += (isVersionable() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        if (getMIMEType() != null) {
            _hashCode += getMIMEType().hashCode();
        }
        if (getFormatURI() != null) {
            _hashCode += getFormatURI().hashCode();
        }
        if (getCreateDate() != null) {
            _hashCode += getCreateDate().hashCode();
        }
        _hashCode += new Long(getSize()).hashCode();
        if (getState() != null) {
            _hashCode += getState().hashCode();
        }
        if (getLocation() != null) {
            _hashCode += getLocation().hashCode();
        }
        if (getChecksumType() != null) {
            _hashCode += getChecksumType().hashCode();
        }
        if (getChecksum() != null) {
            _hashCode += getChecksum().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Datastream.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "Datastream"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("controlGroup");
        elemField.setXmlName(new javax.xml.namespace.QName("", "controlGroup"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "DatastreamControlGroup"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ID");
        elemField.setXmlName(new javax.xml.namespace.QName("", "ID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("versionID");
        elemField.setXmlName(new javax.xml.namespace.QName("", "versionID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("altIDs");
        elemField.setXmlName(new javax.xml.namespace.QName("", "altIDs"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/api/", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("label");
        elemField.setXmlName(new javax.xml.namespace.QName("", "label"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("versionable");
        elemField.setXmlName(new javax.xml.namespace.QName("", "versionable"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("MIMEType");
        elemField.setXmlName(new javax.xml.namespace.QName("", "MIMEType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("formatURI");
        elemField.setXmlName(new javax.xml.namespace.QName("", "formatURI"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("createDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "createDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("size");
        elemField.setXmlName(new javax.xml.namespace.QName("", "size"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("state");
        elemField.setXmlName(new javax.xml.namespace.QName("", "state"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("location");
        elemField.setXmlName(new javax.xml.namespace.QName("", "location"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("checksumType");
        elemField.setXmlName(new javax.xml.namespace.QName("", "checksumType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("checksum");
        elemField.setXmlName(new javax.xml.namespace.QName("", "checksum"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
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
