/**
 * DatastreamBindingMap.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class DatastreamBindingMap  implements java.io.Serializable {
    private java.lang.String dsBindMapID;

    private java.lang.String dsBindMechanismPID;

    private java.lang.String dsBindMapLabel;

    private java.lang.String state;

    private org.purl.sword.server.fedora.api.DatastreamBinding[] dsBindings;

    public DatastreamBindingMap() {
    }

    public DatastreamBindingMap(
           java.lang.String dsBindMapID,
           java.lang.String dsBindMechanismPID,
           java.lang.String dsBindMapLabel,
           java.lang.String state,
           org.purl.sword.server.fedora.api.DatastreamBinding[] dsBindings) {
           this.dsBindMapID = dsBindMapID;
           this.dsBindMechanismPID = dsBindMechanismPID;
           this.dsBindMapLabel = dsBindMapLabel;
           this.state = state;
           this.dsBindings = dsBindings;
    }


    /**
     * Gets the dsBindMapID value for this DatastreamBindingMap.
     * 
     * @return dsBindMapID
     */
    public java.lang.String getDsBindMapID() {
        return dsBindMapID;
    }


    /**
     * Sets the dsBindMapID value for this DatastreamBindingMap.
     * 
     * @param dsBindMapID
     */
    public void setDsBindMapID(java.lang.String dsBindMapID) {
        this.dsBindMapID = dsBindMapID;
    }


    /**
     * Gets the dsBindMechanismPID value for this DatastreamBindingMap.
     * 
     * @return dsBindMechanismPID
     */
    public java.lang.String getDsBindMechanismPID() {
        return dsBindMechanismPID;
    }


    /**
     * Sets the dsBindMechanismPID value for this DatastreamBindingMap.
     * 
     * @param dsBindMechanismPID
     */
    public void setDsBindMechanismPID(java.lang.String dsBindMechanismPID) {
        this.dsBindMechanismPID = dsBindMechanismPID;
    }


    /**
     * Gets the dsBindMapLabel value for this DatastreamBindingMap.
     * 
     * @return dsBindMapLabel
     */
    public java.lang.String getDsBindMapLabel() {
        return dsBindMapLabel;
    }


    /**
     * Sets the dsBindMapLabel value for this DatastreamBindingMap.
     * 
     * @param dsBindMapLabel
     */
    public void setDsBindMapLabel(java.lang.String dsBindMapLabel) {
        this.dsBindMapLabel = dsBindMapLabel;
    }


    /**
     * Gets the state value for this DatastreamBindingMap.
     * 
     * @return state
     */
    public java.lang.String getState() {
        return state;
    }


    /**
     * Sets the state value for this DatastreamBindingMap.
     * 
     * @param state
     */
    public void setState(java.lang.String state) {
        this.state = state;
    }


    /**
     * Gets the dsBindings value for this DatastreamBindingMap.
     * 
     * @return dsBindings
     */
    public org.purl.sword.server.fedora.api.DatastreamBinding[] getDsBindings() {
        return dsBindings;
    }


    /**
     * Sets the dsBindings value for this DatastreamBindingMap.
     * 
     * @param dsBindings
     */
    public void setDsBindings(org.purl.sword.server.fedora.api.DatastreamBinding[] dsBindings) {
        this.dsBindings = dsBindings;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DatastreamBindingMap)) return false;
        DatastreamBindingMap other = (DatastreamBindingMap) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.dsBindMapID==null && other.getDsBindMapID()==null) || 
             (this.dsBindMapID!=null &&
              this.dsBindMapID.equals(other.getDsBindMapID()))) &&
            ((this.dsBindMechanismPID==null && other.getDsBindMechanismPID()==null) || 
             (this.dsBindMechanismPID!=null &&
              this.dsBindMechanismPID.equals(other.getDsBindMechanismPID()))) &&
            ((this.dsBindMapLabel==null && other.getDsBindMapLabel()==null) || 
             (this.dsBindMapLabel!=null &&
              this.dsBindMapLabel.equals(other.getDsBindMapLabel()))) &&
            ((this.state==null && other.getState()==null) || 
             (this.state!=null &&
              this.state.equals(other.getState()))) &&
            ((this.dsBindings==null && other.getDsBindings()==null) || 
             (this.dsBindings!=null &&
              java.util.Arrays.equals(this.dsBindings, other.getDsBindings())));
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
        if (getDsBindMapID() != null) {
            _hashCode += getDsBindMapID().hashCode();
        }
        if (getDsBindMechanismPID() != null) {
            _hashCode += getDsBindMechanismPID().hashCode();
        }
        if (getDsBindMapLabel() != null) {
            _hashCode += getDsBindMapLabel().hashCode();
        }
        if (getState() != null) {
            _hashCode += getState().hashCode();
        }
        if (getDsBindings() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getDsBindings());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getDsBindings(), i);
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
        new org.apache.axis.description.TypeDesc(DatastreamBindingMap.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "DatastreamBindingMap"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dsBindMapID");
        elemField.setXmlName(new javax.xml.namespace.QName("", "dsBindMapID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dsBindMechanismPID");
        elemField.setXmlName(new javax.xml.namespace.QName("", "dsBindMechanismPID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dsBindMapLabel");
        elemField.setXmlName(new javax.xml.namespace.QName("", "dsBindMapLabel"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("state");
        elemField.setXmlName(new javax.xml.namespace.QName("", "state"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dsBindings");
        elemField.setXmlName(new javax.xml.namespace.QName("", "dsBindings"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "DatastreamBinding"));
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
