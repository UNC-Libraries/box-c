/**
 * Disseminator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class Disseminator  implements java.io.Serializable {
    private java.lang.String bDefPID;

    private java.lang.String bMechPID;

    private java.lang.String createDate;

    private java.lang.String ID;

    private java.lang.String label;

    private java.lang.String versionID;

    private org.purl.sword.server.fedora.api.DatastreamBindingMap dsBindMap;

    private java.lang.String state;

    public Disseminator() {
    }

    public Disseminator(
           java.lang.String bDefPID,
           java.lang.String bMechPID,
           java.lang.String createDate,
           java.lang.String ID,
           java.lang.String label,
           java.lang.String versionID,
           org.purl.sword.server.fedora.api.DatastreamBindingMap dsBindMap,
           java.lang.String state) {
           this.bDefPID = bDefPID;
           this.bMechPID = bMechPID;
           this.createDate = createDate;
           this.ID = ID;
           this.label = label;
           this.versionID = versionID;
           this.dsBindMap = dsBindMap;
           this.state = state;
    }


    /**
     * Gets the bDefPID value for this Disseminator.
     * 
     * @return bDefPID
     */
    public java.lang.String getBDefPID() {
        return bDefPID;
    }


    /**
     * Sets the bDefPID value for this Disseminator.
     * 
     * @param bDefPID
     */
    public void setBDefPID(java.lang.String bDefPID) {
        this.bDefPID = bDefPID;
    }


    /**
     * Gets the bMechPID value for this Disseminator.
     * 
     * @return bMechPID
     */
    public java.lang.String getBMechPID() {
        return bMechPID;
    }


    /**
     * Sets the bMechPID value for this Disseminator.
     * 
     * @param bMechPID
     */
    public void setBMechPID(java.lang.String bMechPID) {
        this.bMechPID = bMechPID;
    }


    /**
     * Gets the createDate value for this Disseminator.
     * 
     * @return createDate
     */
    public java.lang.String getCreateDate() {
        return createDate;
    }


    /**
     * Sets the createDate value for this Disseminator.
     * 
     * @param createDate
     */
    public void setCreateDate(java.lang.String createDate) {
        this.createDate = createDate;
    }


    /**
     * Gets the ID value for this Disseminator.
     * 
     * @return ID
     */
    public java.lang.String getID() {
        return ID;
    }


    /**
     * Sets the ID value for this Disseminator.
     * 
     * @param ID
     */
    public void setID(java.lang.String ID) {
        this.ID = ID;
    }


    /**
     * Gets the label value for this Disseminator.
     * 
     * @return label
     */
    public java.lang.String getLabel() {
        return label;
    }


    /**
     * Sets the label value for this Disseminator.
     * 
     * @param label
     */
    public void setLabel(java.lang.String label) {
        this.label = label;
    }


    /**
     * Gets the versionID value for this Disseminator.
     * 
     * @return versionID
     */
    public java.lang.String getVersionID() {
        return versionID;
    }


    /**
     * Sets the versionID value for this Disseminator.
     * 
     * @param versionID
     */
    public void setVersionID(java.lang.String versionID) {
        this.versionID = versionID;
    }


    /**
     * Gets the dsBindMap value for this Disseminator.
     * 
     * @return dsBindMap
     */
    public org.purl.sword.server.fedora.api.DatastreamBindingMap getDsBindMap() {
        return dsBindMap;
    }


    /**
     * Sets the dsBindMap value for this Disseminator.
     * 
     * @param dsBindMap
     */
    public void setDsBindMap(org.purl.sword.server.fedora.api.DatastreamBindingMap dsBindMap) {
        this.dsBindMap = dsBindMap;
    }


    /**
     * Gets the state value for this Disseminator.
     * 
     * @return state
     */
    public java.lang.String getState() {
        return state;
    }


    /**
     * Sets the state value for this Disseminator.
     * 
     * @param state
     */
    public void setState(java.lang.String state) {
        this.state = state;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Disseminator)) return false;
        Disseminator other = (Disseminator) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.bDefPID==null && other.getBDefPID()==null) || 
             (this.bDefPID!=null &&
              this.bDefPID.equals(other.getBDefPID()))) &&
            ((this.bMechPID==null && other.getBMechPID()==null) || 
             (this.bMechPID!=null &&
              this.bMechPID.equals(other.getBMechPID()))) &&
            ((this.createDate==null && other.getCreateDate()==null) || 
             (this.createDate!=null &&
              this.createDate.equals(other.getCreateDate()))) &&
            ((this.ID==null && other.getID()==null) || 
             (this.ID!=null &&
              this.ID.equals(other.getID()))) &&
            ((this.label==null && other.getLabel()==null) || 
             (this.label!=null &&
              this.label.equals(other.getLabel()))) &&
            ((this.versionID==null && other.getVersionID()==null) || 
             (this.versionID!=null &&
              this.versionID.equals(other.getVersionID()))) &&
            ((this.dsBindMap==null && other.getDsBindMap()==null) || 
             (this.dsBindMap!=null &&
              this.dsBindMap.equals(other.getDsBindMap()))) &&
            ((this.state==null && other.getState()==null) || 
             (this.state!=null &&
              this.state.equals(other.getState())));
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
        if (getBDefPID() != null) {
            _hashCode += getBDefPID().hashCode();
        }
        if (getBMechPID() != null) {
            _hashCode += getBMechPID().hashCode();
        }
        if (getCreateDate() != null) {
            _hashCode += getCreateDate().hashCode();
        }
        if (getID() != null) {
            _hashCode += getID().hashCode();
        }
        if (getLabel() != null) {
            _hashCode += getLabel().hashCode();
        }
        if (getVersionID() != null) {
            _hashCode += getVersionID().hashCode();
        }
        if (getDsBindMap() != null) {
            _hashCode += getDsBindMap().hashCode();
        }
        if (getState() != null) {
            _hashCode += getState().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Disseminator.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "Disseminator"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("BDefPID");
        elemField.setXmlName(new javax.xml.namespace.QName("", "bDefPID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("BMechPID");
        elemField.setXmlName(new javax.xml.namespace.QName("", "bMechPID"));
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
        elemField.setFieldName("ID");
        elemField.setXmlName(new javax.xml.namespace.QName("", "ID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("label");
        elemField.setXmlName(new javax.xml.namespace.QName("", "label"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("versionID");
        elemField.setXmlName(new javax.xml.namespace.QName("", "versionID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dsBindMap");
        elemField.setXmlName(new javax.xml.namespace.QName("", "dsBindMap"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "DatastreamBindingMap"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("state");
        elemField.setXmlName(new javax.xml.namespace.QName("", "state"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
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
