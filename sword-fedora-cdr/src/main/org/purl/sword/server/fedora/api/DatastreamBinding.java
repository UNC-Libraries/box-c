/**
 * DatastreamBinding.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class DatastreamBinding  implements java.io.Serializable {
    private java.lang.String bindKeyName;

    private java.lang.String bindLabel;

    private java.lang.String datastreamID;

    private java.lang.String seqNo;

    public DatastreamBinding() {
    }

    public DatastreamBinding(
           java.lang.String bindKeyName,
           java.lang.String bindLabel,
           java.lang.String datastreamID,
           java.lang.String seqNo) {
           this.bindKeyName = bindKeyName;
           this.bindLabel = bindLabel;
           this.datastreamID = datastreamID;
           this.seqNo = seqNo;
    }


    /**
     * Gets the bindKeyName value for this DatastreamBinding.
     * 
     * @return bindKeyName
     */
    public java.lang.String getBindKeyName() {
        return bindKeyName;
    }


    /**
     * Sets the bindKeyName value for this DatastreamBinding.
     * 
     * @param bindKeyName
     */
    public void setBindKeyName(java.lang.String bindKeyName) {
        this.bindKeyName = bindKeyName;
    }


    /**
     * Gets the bindLabel value for this DatastreamBinding.
     * 
     * @return bindLabel
     */
    public java.lang.String getBindLabel() {
        return bindLabel;
    }


    /**
     * Sets the bindLabel value for this DatastreamBinding.
     * 
     * @param bindLabel
     */
    public void setBindLabel(java.lang.String bindLabel) {
        this.bindLabel = bindLabel;
    }


    /**
     * Gets the datastreamID value for this DatastreamBinding.
     * 
     * @return datastreamID
     */
    public java.lang.String getDatastreamID() {
        return datastreamID;
    }


    /**
     * Sets the datastreamID value for this DatastreamBinding.
     * 
     * @param datastreamID
     */
    public void setDatastreamID(java.lang.String datastreamID) {
        this.datastreamID = datastreamID;
    }


    /**
     * Gets the seqNo value for this DatastreamBinding.
     * 
     * @return seqNo
     */
    public java.lang.String getSeqNo() {
        return seqNo;
    }


    /**
     * Sets the seqNo value for this DatastreamBinding.
     * 
     * @param seqNo
     */
    public void setSeqNo(java.lang.String seqNo) {
        this.seqNo = seqNo;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DatastreamBinding)) return false;
        DatastreamBinding other = (DatastreamBinding) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.bindKeyName==null && other.getBindKeyName()==null) || 
             (this.bindKeyName!=null &&
              this.bindKeyName.equals(other.getBindKeyName()))) &&
            ((this.bindLabel==null && other.getBindLabel()==null) || 
             (this.bindLabel!=null &&
              this.bindLabel.equals(other.getBindLabel()))) &&
            ((this.datastreamID==null && other.getDatastreamID()==null) || 
             (this.datastreamID!=null &&
              this.datastreamID.equals(other.getDatastreamID()))) &&
            ((this.seqNo==null && other.getSeqNo()==null) || 
             (this.seqNo!=null &&
              this.seqNo.equals(other.getSeqNo())));
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
        if (getBindKeyName() != null) {
            _hashCode += getBindKeyName().hashCode();
        }
        if (getBindLabel() != null) {
            _hashCode += getBindLabel().hashCode();
        }
        if (getDatastreamID() != null) {
            _hashCode += getDatastreamID().hashCode();
        }
        if (getSeqNo() != null) {
            _hashCode += getSeqNo().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DatastreamBinding.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "DatastreamBinding"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("bindKeyName");
        elemField.setXmlName(new javax.xml.namespace.QName("", "bindKeyName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("bindLabel");
        elemField.setXmlName(new javax.xml.namespace.QName("", "bindLabel"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("datastreamID");
        elemField.setXmlName(new javax.xml.namespace.QName("", "datastreamID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("seqNo");
        elemField.setXmlName(new javax.xml.namespace.QName("", "seqNo"));
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
