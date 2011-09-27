/**
 * ObjectProfile.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class ObjectProfile  implements java.io.Serializable {
    private java.lang.String pid;

    private java.lang.String objLabel;

    private java.lang.String objContentModel;

    private java.lang.String objType;

    private java.lang.String objCreateDate;

    private java.lang.String objLastModDate;

    private java.lang.String objDissIndexViewURL;

    private java.lang.String objItemIndexViewURL;

    public ObjectProfile() {
    }

    public ObjectProfile(
           java.lang.String pid,
           java.lang.String objLabel,
           java.lang.String objContentModel,
           java.lang.String objType,
           java.lang.String objCreateDate,
           java.lang.String objLastModDate,
           java.lang.String objDissIndexViewURL,
           java.lang.String objItemIndexViewURL) {
           this.pid = pid;
           this.objLabel = objLabel;
           this.objContentModel = objContentModel;
           this.objType = objType;
           this.objCreateDate = objCreateDate;
           this.objLastModDate = objLastModDate;
           this.objDissIndexViewURL = objDissIndexViewURL;
           this.objItemIndexViewURL = objItemIndexViewURL;
    }


    /**
     * Gets the pid value for this ObjectProfile.
     * 
     * @return pid
     */
    public java.lang.String getPid() {
        return pid;
    }


    /**
     * Sets the pid value for this ObjectProfile.
     * 
     * @param pid
     */
    public void setPid(java.lang.String pid) {
        this.pid = pid;
    }


    /**
     * Gets the objLabel value for this ObjectProfile.
     * 
     * @return objLabel
     */
    public java.lang.String getObjLabel() {
        return objLabel;
    }


    /**
     * Sets the objLabel value for this ObjectProfile.
     * 
     * @param objLabel
     */
    public void setObjLabel(java.lang.String objLabel) {
        this.objLabel = objLabel;
    }


    /**
     * Gets the objContentModel value for this ObjectProfile.
     * 
     * @return objContentModel
     */
    public java.lang.String getObjContentModel() {
        return objContentModel;
    }


    /**
     * Sets the objContentModel value for this ObjectProfile.
     * 
     * @param objContentModel
     */
    public void setObjContentModel(java.lang.String objContentModel) {
        this.objContentModel = objContentModel;
    }


    /**
     * Gets the objType value for this ObjectProfile.
     * 
     * @return objType
     */
    public java.lang.String getObjType() {
        return objType;
    }


    /**
     * Sets the objType value for this ObjectProfile.
     * 
     * @param objType
     */
    public void setObjType(java.lang.String objType) {
        this.objType = objType;
    }


    /**
     * Gets the objCreateDate value for this ObjectProfile.
     * 
     * @return objCreateDate
     */
    public java.lang.String getObjCreateDate() {
        return objCreateDate;
    }


    /**
     * Sets the objCreateDate value for this ObjectProfile.
     * 
     * @param objCreateDate
     */
    public void setObjCreateDate(java.lang.String objCreateDate) {
        this.objCreateDate = objCreateDate;
    }


    /**
     * Gets the objLastModDate value for this ObjectProfile.
     * 
     * @return objLastModDate
     */
    public java.lang.String getObjLastModDate() {
        return objLastModDate;
    }


    /**
     * Sets the objLastModDate value for this ObjectProfile.
     * 
     * @param objLastModDate
     */
    public void setObjLastModDate(java.lang.String objLastModDate) {
        this.objLastModDate = objLastModDate;
    }


    /**
     * Gets the objDissIndexViewURL value for this ObjectProfile.
     * 
     * @return objDissIndexViewURL
     */
    public java.lang.String getObjDissIndexViewURL() {
        return objDissIndexViewURL;
    }


    /**
     * Sets the objDissIndexViewURL value for this ObjectProfile.
     * 
     * @param objDissIndexViewURL
     */
    public void setObjDissIndexViewURL(java.lang.String objDissIndexViewURL) {
        this.objDissIndexViewURL = objDissIndexViewURL;
    }


    /**
     * Gets the objItemIndexViewURL value for this ObjectProfile.
     * 
     * @return objItemIndexViewURL
     */
    public java.lang.String getObjItemIndexViewURL() {
        return objItemIndexViewURL;
    }


    /**
     * Sets the objItemIndexViewURL value for this ObjectProfile.
     * 
     * @param objItemIndexViewURL
     */
    public void setObjItemIndexViewURL(java.lang.String objItemIndexViewURL) {
        this.objItemIndexViewURL = objItemIndexViewURL;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ObjectProfile)) return false;
        ObjectProfile other = (ObjectProfile) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.pid==null && other.getPid()==null) || 
             (this.pid!=null &&
              this.pid.equals(other.getPid()))) &&
            ((this.objLabel==null && other.getObjLabel()==null) || 
             (this.objLabel!=null &&
              this.objLabel.equals(other.getObjLabel()))) &&
            ((this.objContentModel==null && other.getObjContentModel()==null) || 
             (this.objContentModel!=null &&
              this.objContentModel.equals(other.getObjContentModel()))) &&
            ((this.objType==null && other.getObjType()==null) || 
             (this.objType!=null &&
              this.objType.equals(other.getObjType()))) &&
            ((this.objCreateDate==null && other.getObjCreateDate()==null) || 
             (this.objCreateDate!=null &&
              this.objCreateDate.equals(other.getObjCreateDate()))) &&
            ((this.objLastModDate==null && other.getObjLastModDate()==null) || 
             (this.objLastModDate!=null &&
              this.objLastModDate.equals(other.getObjLastModDate()))) &&
            ((this.objDissIndexViewURL==null && other.getObjDissIndexViewURL()==null) || 
             (this.objDissIndexViewURL!=null &&
              this.objDissIndexViewURL.equals(other.getObjDissIndexViewURL()))) &&
            ((this.objItemIndexViewURL==null && other.getObjItemIndexViewURL()==null) || 
             (this.objItemIndexViewURL!=null &&
              this.objItemIndexViewURL.equals(other.getObjItemIndexViewURL())));
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
        if (getPid() != null) {
            _hashCode += getPid().hashCode();
        }
        if (getObjLabel() != null) {
            _hashCode += getObjLabel().hashCode();
        }
        if (getObjContentModel() != null) {
            _hashCode += getObjContentModel().hashCode();
        }
        if (getObjType() != null) {
            _hashCode += getObjType().hashCode();
        }
        if (getObjCreateDate() != null) {
            _hashCode += getObjCreateDate().hashCode();
        }
        if (getObjLastModDate() != null) {
            _hashCode += getObjLastModDate().hashCode();
        }
        if (getObjDissIndexViewURL() != null) {
            _hashCode += getObjDissIndexViewURL().hashCode();
        }
        if (getObjItemIndexViewURL() != null) {
            _hashCode += getObjItemIndexViewURL().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ObjectProfile.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "ObjectProfile"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("pid");
        elemField.setXmlName(new javax.xml.namespace.QName("", "pid"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("objLabel");
        elemField.setXmlName(new javax.xml.namespace.QName("", "objLabel"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("objContentModel");
        elemField.setXmlName(new javax.xml.namespace.QName("", "objContentModel"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("objType");
        elemField.setXmlName(new javax.xml.namespace.QName("", "objType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("objCreateDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "objCreateDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("objLastModDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "objLastModDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("objDissIndexViewURL");
        elemField.setXmlName(new javax.xml.namespace.QName("", "objDissIndexViewURL"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("objItemIndexViewURL");
        elemField.setXmlName(new javax.xml.namespace.QName("", "objItemIndexViewURL"));
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
