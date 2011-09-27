/**
 * MIMETypedStream.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class MIMETypedStream  implements java.io.Serializable {
    private java.lang.String MIMEType;

    private byte[] stream;

    private org.purl.sword.server.fedora.api.Property[] header;

    public MIMETypedStream() {
    }

    public MIMETypedStream(
           java.lang.String MIMEType,
           byte[] stream,
           org.purl.sword.server.fedora.api.Property[] header) {
           this.MIMEType = MIMEType;
           this.stream = stream;
           this.header = header;
    }


    /**
     * Gets the MIMEType value for this MIMETypedStream.
     * 
     * @return MIMEType
     */
    public java.lang.String getMIMEType() {
        return MIMEType;
    }


    /**
     * Sets the MIMEType value for this MIMETypedStream.
     * 
     * @param MIMEType
     */
    public void setMIMEType(java.lang.String MIMEType) {
        this.MIMEType = MIMEType;
    }


    /**
     * Gets the stream value for this MIMETypedStream.
     * 
     * @return stream
     */
    public byte[] getStream() {
        return stream;
    }


    /**
     * Sets the stream value for this MIMETypedStream.
     * 
     * @param stream
     */
    public void setStream(byte[] stream) {
        this.stream = stream;
    }


    /**
     * Gets the header value for this MIMETypedStream.
     * 
     * @return header
     */
    public org.purl.sword.server.fedora.api.Property[] getHeader() {
        return header;
    }


    /**
     * Sets the header value for this MIMETypedStream.
     * 
     * @param header
     */
    public void setHeader(org.purl.sword.server.fedora.api.Property[] header) {
        this.header = header;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof MIMETypedStream)) return false;
        MIMETypedStream other = (MIMETypedStream) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.MIMEType==null && other.getMIMEType()==null) || 
             (this.MIMEType!=null &&
              this.MIMEType.equals(other.getMIMEType()))) &&
            ((this.stream==null && other.getStream()==null) || 
             (this.stream!=null &&
              java.util.Arrays.equals(this.stream, other.getStream()))) &&
            ((this.header==null && other.getHeader()==null) || 
             (this.header!=null &&
              java.util.Arrays.equals(this.header, other.getHeader())));
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
        if (getMIMEType() != null) {
            _hashCode += getMIMEType().hashCode();
        }
        if (getStream() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getStream());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getStream(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getHeader() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getHeader());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getHeader(), i);
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
        new org.apache.axis.description.TypeDesc(MIMETypedStream.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "MIMETypedStream"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("MIMEType");
        elemField.setXmlName(new javax.xml.namespace.QName("", "MIMEType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("stream");
        elemField.setXmlName(new javax.xml.namespace.QName("", "stream"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "base64Binary"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("header");
        elemField.setXmlName(new javax.xml.namespace.QName("", "header"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "Property"));
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
