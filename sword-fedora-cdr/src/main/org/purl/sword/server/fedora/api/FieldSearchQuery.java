/**
 * FieldSearchQuery.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class FieldSearchQuery  implements java.io.Serializable {
    private org.purl.sword.server.fedora.api.Condition[] conditions;

    private java.lang.String terms;

    public FieldSearchQuery() {
    }

    public FieldSearchQuery(
           org.purl.sword.server.fedora.api.Condition[] conditions,
           java.lang.String terms) {
           this.conditions = conditions;
           this.terms = terms;
    }


    /**
     * Gets the conditions value for this FieldSearchQuery.
     * 
     * @return conditions
     */
    public org.purl.sword.server.fedora.api.Condition[] getConditions() {
        return conditions;
    }


    /**
     * Sets the conditions value for this FieldSearchQuery.
     * 
     * @param conditions
     */
    public void setConditions(org.purl.sword.server.fedora.api.Condition[] conditions) {
        this.conditions = conditions;
    }


    /**
     * Gets the terms value for this FieldSearchQuery.
     * 
     * @return terms
     */
    public java.lang.String getTerms() {
        return terms;
    }


    /**
     * Sets the terms value for this FieldSearchQuery.
     * 
     * @param terms
     */
    public void setTerms(java.lang.String terms) {
        this.terms = terms;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof FieldSearchQuery)) return false;
        FieldSearchQuery other = (FieldSearchQuery) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.conditions==null && other.getConditions()==null) || 
             (this.conditions!=null &&
              java.util.Arrays.equals(this.conditions, other.getConditions()))) &&
            ((this.terms==null && other.getTerms()==null) || 
             (this.terms!=null &&
              this.terms.equals(other.getTerms())));
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
        if (getConditions() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getConditions());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getConditions(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getTerms() != null) {
            _hashCode += getTerms().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(FieldSearchQuery.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "FieldSearchQuery"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("conditions");
        elemField.setXmlName(new javax.xml.namespace.QName("", "conditions"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "Condition"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/api/", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("terms");
        elemField.setXmlName(new javax.xml.namespace.QName("", "terms"));
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
