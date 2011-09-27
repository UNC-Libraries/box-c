/**
 * MethodParmDef.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class MethodParmDef  implements java.io.Serializable {
    private java.lang.String parmName;

    private java.lang.String parmType;

    private java.lang.String parmDefaultValue;

    private java.lang.String[] parmDomainValues;

    private boolean parmRequired;

    private java.lang.String parmLabel;

    private java.lang.String parmPassBy;

    private org.purl.sword.server.fedora.api.PassByRef PASS_BY_REF;

    private org.purl.sword.server.fedora.api.PassByValue PASS_BY_VALUE;

    private org.purl.sword.server.fedora.api.DatastreamInputType DATASTREAM_INPUT;

    private org.purl.sword.server.fedora.api.UserInputType USER_INPUT;

    private org.purl.sword.server.fedora.api.DefaultInputType DEFAULT_INPUT;

    public MethodParmDef() {
    }

    public MethodParmDef(
           java.lang.String parmName,
           java.lang.String parmType,
           java.lang.String parmDefaultValue,
           java.lang.String[] parmDomainValues,
           boolean parmRequired,
           java.lang.String parmLabel,
           java.lang.String parmPassBy,
           org.purl.sword.server.fedora.api.PassByRef PASS_BY_REF,
           org.purl.sword.server.fedora.api.PassByValue PASS_BY_VALUE,
           org.purl.sword.server.fedora.api.DatastreamInputType DATASTREAM_INPUT,
           org.purl.sword.server.fedora.api.UserInputType USER_INPUT,
           org.purl.sword.server.fedora.api.DefaultInputType DEFAULT_INPUT) {
           this.parmName = parmName;
           this.parmType = parmType;
           this.parmDefaultValue = parmDefaultValue;
           this.parmDomainValues = parmDomainValues;
           this.parmRequired = parmRequired;
           this.parmLabel = parmLabel;
           this.parmPassBy = parmPassBy;
           this.PASS_BY_REF = PASS_BY_REF;
           this.PASS_BY_VALUE = PASS_BY_VALUE;
           this.DATASTREAM_INPUT = DATASTREAM_INPUT;
           this.USER_INPUT = USER_INPUT;
           this.DEFAULT_INPUT = DEFAULT_INPUT;
    }


    /**
     * Gets the parmName value for this MethodParmDef.
     * 
     * @return parmName
     */
    public java.lang.String getParmName() {
        return parmName;
    }


    /**
     * Sets the parmName value for this MethodParmDef.
     * 
     * @param parmName
     */
    public void setParmName(java.lang.String parmName) {
        this.parmName = parmName;
    }


    /**
     * Gets the parmType value for this MethodParmDef.
     * 
     * @return parmType
     */
    public java.lang.String getParmType() {
        return parmType;
    }


    /**
     * Sets the parmType value for this MethodParmDef.
     * 
     * @param parmType
     */
    public void setParmType(java.lang.String parmType) {
        this.parmType = parmType;
    }


    /**
     * Gets the parmDefaultValue value for this MethodParmDef.
     * 
     * @return parmDefaultValue
     */
    public java.lang.String getParmDefaultValue() {
        return parmDefaultValue;
    }


    /**
     * Sets the parmDefaultValue value for this MethodParmDef.
     * 
     * @param parmDefaultValue
     */
    public void setParmDefaultValue(java.lang.String parmDefaultValue) {
        this.parmDefaultValue = parmDefaultValue;
    }


    /**
     * Gets the parmDomainValues value for this MethodParmDef.
     * 
     * @return parmDomainValues
     */
    public java.lang.String[] getParmDomainValues() {
        return parmDomainValues;
    }


    /**
     * Sets the parmDomainValues value for this MethodParmDef.
     * 
     * @param parmDomainValues
     */
    public void setParmDomainValues(java.lang.String[] parmDomainValues) {
        this.parmDomainValues = parmDomainValues;
    }


    /**
     * Gets the parmRequired value for this MethodParmDef.
     * 
     * @return parmRequired
     */
    public boolean isParmRequired() {
        return parmRequired;
    }


    /**
     * Sets the parmRequired value for this MethodParmDef.
     * 
     * @param parmRequired
     */
    public void setParmRequired(boolean parmRequired) {
        this.parmRequired = parmRequired;
    }


    /**
     * Gets the parmLabel value for this MethodParmDef.
     * 
     * @return parmLabel
     */
    public java.lang.String getParmLabel() {
        return parmLabel;
    }


    /**
     * Sets the parmLabel value for this MethodParmDef.
     * 
     * @param parmLabel
     */
    public void setParmLabel(java.lang.String parmLabel) {
        this.parmLabel = parmLabel;
    }


    /**
     * Gets the parmPassBy value for this MethodParmDef.
     * 
     * @return parmPassBy
     */
    public java.lang.String getParmPassBy() {
        return parmPassBy;
    }


    /**
     * Sets the parmPassBy value for this MethodParmDef.
     * 
     * @param parmPassBy
     */
    public void setParmPassBy(java.lang.String parmPassBy) {
        this.parmPassBy = parmPassBy;
    }


    /**
     * Gets the PASS_BY_REF value for this MethodParmDef.
     * 
     * @return PASS_BY_REF
     */
    public org.purl.sword.server.fedora.api.PassByRef getPASS_BY_REF() {
        return PASS_BY_REF;
    }


    /**
     * Sets the PASS_BY_REF value for this MethodParmDef.
     * 
     * @param PASS_BY_REF
     */
    public void setPASS_BY_REF(org.purl.sword.server.fedora.api.PassByRef PASS_BY_REF) {
        this.PASS_BY_REF = PASS_BY_REF;
    }


    /**
     * Gets the PASS_BY_VALUE value for this MethodParmDef.
     * 
     * @return PASS_BY_VALUE
     */
    public org.purl.sword.server.fedora.api.PassByValue getPASS_BY_VALUE() {
        return PASS_BY_VALUE;
    }


    /**
     * Sets the PASS_BY_VALUE value for this MethodParmDef.
     * 
     * @param PASS_BY_VALUE
     */
    public void setPASS_BY_VALUE(org.purl.sword.server.fedora.api.PassByValue PASS_BY_VALUE) {
        this.PASS_BY_VALUE = PASS_BY_VALUE;
    }


    /**
     * Gets the DATASTREAM_INPUT value for this MethodParmDef.
     * 
     * @return DATASTREAM_INPUT
     */
    public org.purl.sword.server.fedora.api.DatastreamInputType getDATASTREAM_INPUT() {
        return DATASTREAM_INPUT;
    }


    /**
     * Sets the DATASTREAM_INPUT value for this MethodParmDef.
     * 
     * @param DATASTREAM_INPUT
     */
    public void setDATASTREAM_INPUT(org.purl.sword.server.fedora.api.DatastreamInputType DATASTREAM_INPUT) {
        this.DATASTREAM_INPUT = DATASTREAM_INPUT;
    }


    /**
     * Gets the USER_INPUT value for this MethodParmDef.
     * 
     * @return USER_INPUT
     */
    public org.purl.sword.server.fedora.api.UserInputType getUSER_INPUT() {
        return USER_INPUT;
    }


    /**
     * Sets the USER_INPUT value for this MethodParmDef.
     * 
     * @param USER_INPUT
     */
    public void setUSER_INPUT(org.purl.sword.server.fedora.api.UserInputType USER_INPUT) {
        this.USER_INPUT = USER_INPUT;
    }


    /**
     * Gets the DEFAULT_INPUT value for this MethodParmDef.
     * 
     * @return DEFAULT_INPUT
     */
    public org.purl.sword.server.fedora.api.DefaultInputType getDEFAULT_INPUT() {
        return DEFAULT_INPUT;
    }


    /**
     * Sets the DEFAULT_INPUT value for this MethodParmDef.
     * 
     * @param DEFAULT_INPUT
     */
    public void setDEFAULT_INPUT(org.purl.sword.server.fedora.api.DefaultInputType DEFAULT_INPUT) {
        this.DEFAULT_INPUT = DEFAULT_INPUT;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof MethodParmDef)) return false;
        MethodParmDef other = (MethodParmDef) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.parmName==null && other.getParmName()==null) || 
             (this.parmName!=null &&
              this.parmName.equals(other.getParmName()))) &&
            ((this.parmType==null && other.getParmType()==null) || 
             (this.parmType!=null &&
              this.parmType.equals(other.getParmType()))) &&
            ((this.parmDefaultValue==null && other.getParmDefaultValue()==null) || 
             (this.parmDefaultValue!=null &&
              this.parmDefaultValue.equals(other.getParmDefaultValue()))) &&
            ((this.parmDomainValues==null && other.getParmDomainValues()==null) || 
             (this.parmDomainValues!=null &&
              java.util.Arrays.equals(this.parmDomainValues, other.getParmDomainValues()))) &&
            this.parmRequired == other.isParmRequired() &&
            ((this.parmLabel==null && other.getParmLabel()==null) || 
             (this.parmLabel!=null &&
              this.parmLabel.equals(other.getParmLabel()))) &&
            ((this.parmPassBy==null && other.getParmPassBy()==null) || 
             (this.parmPassBy!=null &&
              this.parmPassBy.equals(other.getParmPassBy()))) &&
            ((this.PASS_BY_REF==null && other.getPASS_BY_REF()==null) || 
             (this.PASS_BY_REF!=null &&
              this.PASS_BY_REF.equals(other.getPASS_BY_REF()))) &&
            ((this.PASS_BY_VALUE==null && other.getPASS_BY_VALUE()==null) || 
             (this.PASS_BY_VALUE!=null &&
              this.PASS_BY_VALUE.equals(other.getPASS_BY_VALUE()))) &&
            ((this.DATASTREAM_INPUT==null && other.getDATASTREAM_INPUT()==null) || 
             (this.DATASTREAM_INPUT!=null &&
              this.DATASTREAM_INPUT.equals(other.getDATASTREAM_INPUT()))) &&
            ((this.USER_INPUT==null && other.getUSER_INPUT()==null) || 
             (this.USER_INPUT!=null &&
              this.USER_INPUT.equals(other.getUSER_INPUT()))) &&
            ((this.DEFAULT_INPUT==null && other.getDEFAULT_INPUT()==null) || 
             (this.DEFAULT_INPUT!=null &&
              this.DEFAULT_INPUT.equals(other.getDEFAULT_INPUT())));
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
        if (getParmName() != null) {
            _hashCode += getParmName().hashCode();
        }
        if (getParmType() != null) {
            _hashCode += getParmType().hashCode();
        }
        if (getParmDefaultValue() != null) {
            _hashCode += getParmDefaultValue().hashCode();
        }
        if (getParmDomainValues() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getParmDomainValues());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getParmDomainValues(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        _hashCode += (isParmRequired() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        if (getParmLabel() != null) {
            _hashCode += getParmLabel().hashCode();
        }
        if (getParmPassBy() != null) {
            _hashCode += getParmPassBy().hashCode();
        }
        if (getPASS_BY_REF() != null) {
            _hashCode += getPASS_BY_REF().hashCode();
        }
        if (getPASS_BY_VALUE() != null) {
            _hashCode += getPASS_BY_VALUE().hashCode();
        }
        if (getDATASTREAM_INPUT() != null) {
            _hashCode += getDATASTREAM_INPUT().hashCode();
        }
        if (getUSER_INPUT() != null) {
            _hashCode += getUSER_INPUT().hashCode();
        }
        if (getDEFAULT_INPUT() != null) {
            _hashCode += getDEFAULT_INPUT().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(MethodParmDef.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "MethodParmDef"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parmName");
        elemField.setXmlName(new javax.xml.namespace.QName("", "parmName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parmType");
        elemField.setXmlName(new javax.xml.namespace.QName("", "parmType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parmDefaultValue");
        elemField.setXmlName(new javax.xml.namespace.QName("", "parmDefaultValue"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parmDomainValues");
        elemField.setXmlName(new javax.xml.namespace.QName("", "parmDomainValues"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        elemField.setItemQName(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/api/", "item"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parmRequired");
        elemField.setXmlName(new javax.xml.namespace.QName("", "parmRequired"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parmLabel");
        elemField.setXmlName(new javax.xml.namespace.QName("", "parmLabel"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parmPassBy");
        elemField.setXmlName(new javax.xml.namespace.QName("", "parmPassBy"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("PASS_BY_REF");
        elemField.setXmlName(new javax.xml.namespace.QName("", "PASS_BY_REF"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "passByRef"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("PASS_BY_VALUE");
        elemField.setXmlName(new javax.xml.namespace.QName("", "PASS_BY_VALUE"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "passByValue"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("DATASTREAM_INPUT");
        elemField.setXmlName(new javax.xml.namespace.QName("", "DATASTREAM_INPUT"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "datastreamInputType"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("USER_INPUT");
        elemField.setXmlName(new javax.xml.namespace.QName("", "USER_INPUT"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "userInputType"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("DEFAULT_INPUT");
        elemField.setXmlName(new javax.xml.namespace.QName("", "DEFAULT_INPUT"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "defaultInputType"));
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
