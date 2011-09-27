/**
 * ListSession.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class ListSession  implements java.io.Serializable {
    private java.lang.String token;

    private org.apache.axis.types.NonNegativeInteger cursor;

    private org.apache.axis.types.NonNegativeInteger completeListSize;

    private java.lang.String expirationDate;

    public ListSession() {
    }

    public ListSession(
           java.lang.String token,
           org.apache.axis.types.NonNegativeInteger cursor,
           org.apache.axis.types.NonNegativeInteger completeListSize,
           java.lang.String expirationDate) {
           this.token = token;
           this.cursor = cursor;
           this.completeListSize = completeListSize;
           this.expirationDate = expirationDate;
    }


    /**
     * Gets the token value for this ListSession.
     * 
     * @return token
     */
    public java.lang.String getToken() {
        return token;
    }


    /**
     * Sets the token value for this ListSession.
     * 
     * @param token
     */
    public void setToken(java.lang.String token) {
        this.token = token;
    }


    /**
     * Gets the cursor value for this ListSession.
     * 
     * @return cursor
     */
    public org.apache.axis.types.NonNegativeInteger getCursor() {
        return cursor;
    }


    /**
     * Sets the cursor value for this ListSession.
     * 
     * @param cursor
     */
    public void setCursor(org.apache.axis.types.NonNegativeInteger cursor) {
        this.cursor = cursor;
    }


    /**
     * Gets the completeListSize value for this ListSession.
     * 
     * @return completeListSize
     */
    public org.apache.axis.types.NonNegativeInteger getCompleteListSize() {
        return completeListSize;
    }


    /**
     * Sets the completeListSize value for this ListSession.
     * 
     * @param completeListSize
     */
    public void setCompleteListSize(org.apache.axis.types.NonNegativeInteger completeListSize) {
        this.completeListSize = completeListSize;
    }


    /**
     * Gets the expirationDate value for this ListSession.
     * 
     * @return expirationDate
     */
    public java.lang.String getExpirationDate() {
        return expirationDate;
    }


    /**
     * Sets the expirationDate value for this ListSession.
     * 
     * @param expirationDate
     */
    public void setExpirationDate(java.lang.String expirationDate) {
        this.expirationDate = expirationDate;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ListSession)) return false;
        ListSession other = (ListSession) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.token==null && other.getToken()==null) || 
             (this.token!=null &&
              this.token.equals(other.getToken()))) &&
            ((this.cursor==null && other.getCursor()==null) || 
             (this.cursor!=null &&
              this.cursor.equals(other.getCursor()))) &&
            ((this.completeListSize==null && other.getCompleteListSize()==null) || 
             (this.completeListSize!=null &&
              this.completeListSize.equals(other.getCompleteListSize()))) &&
            ((this.expirationDate==null && other.getExpirationDate()==null) || 
             (this.expirationDate!=null &&
              this.expirationDate.equals(other.getExpirationDate())));
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
        if (getToken() != null) {
            _hashCode += getToken().hashCode();
        }
        if (getCursor() != null) {
            _hashCode += getCursor().hashCode();
        }
        if (getCompleteListSize() != null) {
            _hashCode += getCompleteListSize().hashCode();
        }
        if (getExpirationDate() != null) {
            _hashCode += getExpirationDate().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ListSession.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "ListSession"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("token");
        elemField.setXmlName(new javax.xml.namespace.QName("", "token"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("cursor");
        elemField.setXmlName(new javax.xml.namespace.QName("", "cursor"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "nonNegativeInteger"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("completeListSize");
        elemField.setXmlName(new javax.xml.namespace.QName("", "completeListSize"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "nonNegativeInteger"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("expirationDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "expirationDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
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
