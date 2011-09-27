/**
 * DatastreamControlGroup.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class DatastreamControlGroup implements java.io.Serializable {
    private java.lang.String _value_;
    private static java.util.HashMap _table_ = new java.util.HashMap();

    // Constructor
@SuppressWarnings(value={"unchecked"})
    protected DatastreamControlGroup(java.lang.String value) {
        _value_ = value;
        _table_.put(_value_,this);
    }

    public static final java.lang.String _E = "E";
    public static final java.lang.String _M = "M";
    public static final java.lang.String _X = "X";
    public static final java.lang.String _R = "R";
    public static final DatastreamControlGroup E = new DatastreamControlGroup(_E);
    public static final DatastreamControlGroup M = new DatastreamControlGroup(_M);
    public static final DatastreamControlGroup X = new DatastreamControlGroup(_X);
    public static final DatastreamControlGroup R = new DatastreamControlGroup(_R);
    public java.lang.String getValue() { return _value_;}
    public static DatastreamControlGroup fromValue(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        DatastreamControlGroup enumeration = (DatastreamControlGroup)
            _table_.get(value);
        if (enumeration==null) throw new java.lang.IllegalArgumentException();
        return enumeration;
    }
    public static DatastreamControlGroup fromString(java.lang.String value)
          throws java.lang.IllegalArgumentException {
        return fromValue(value);
    }
    public boolean equals(java.lang.Object obj) {return (obj == this);}
    public int hashCode() { return toString().hashCode();}
    public java.lang.String toString() { return _value_;}
    public java.lang.Object readResolve() throws java.io.ObjectStreamException { return fromValue(_value_);}
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new org.apache.axis.encoding.ser.EnumSerializer(
            _javaType, _xmlType);
    }
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new org.apache.axis.encoding.ser.EnumDeserializer(
            _javaType, _xmlType);
    }
    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DatastreamControlGroup.class);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "DatastreamControlGroup"));
    }
    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

}
