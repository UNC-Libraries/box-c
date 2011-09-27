/**
 * ObjectFields.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.3 Oct 05, 2005 (05:23:37 EDT) WSDL2Java emitter.
 */

package org.purl.sword.server.fedora.api;

public class ObjectFields  implements java.io.Serializable {
    private java.lang.String pid;

    private java.lang.String label;

    private java.lang.String fType;

    private java.lang.String cModel;

    private java.lang.String state;

    private java.lang.String ownerId;

    private java.lang.String cDate;

    private java.lang.String mDate;

    private java.lang.String dcmDate;

    private java.lang.String[] bDef;

    private java.lang.String[] bMech;

    private java.lang.String[] title;

    private java.lang.String[] creator;

    private java.lang.String[] subject;

    private java.lang.String[] description;

    private java.lang.String[] publisher;

    private java.lang.String[] contributor;

    private java.lang.String[] date;

    private java.lang.String[] type;

    private java.lang.String[] format;

    private java.lang.String[] identifier;

    private java.lang.String[] source;

    private java.lang.String[] language;

    private java.lang.String[] relation;

    private java.lang.String[] coverage;

    private java.lang.String[] rights;

    public ObjectFields() {
    }

    public ObjectFields(
           java.lang.String pid,
           java.lang.String label,
           java.lang.String fType,
           java.lang.String cModel,
           java.lang.String state,
           java.lang.String ownerId,
           java.lang.String cDate,
           java.lang.String mDate,
           java.lang.String dcmDate,
           java.lang.String[] bDef,
           java.lang.String[] bMech,
           java.lang.String[] title,
           java.lang.String[] creator,
           java.lang.String[] subject,
           java.lang.String[] description,
           java.lang.String[] publisher,
           java.lang.String[] contributor,
           java.lang.String[] date,
           java.lang.String[] type,
           java.lang.String[] format,
           java.lang.String[] identifier,
           java.lang.String[] source,
           java.lang.String[] language,
           java.lang.String[] relation,
           java.lang.String[] coverage,
           java.lang.String[] rights) {
           this.pid = pid;
           this.label = label;
           this.fType = fType;
           this.cModel = cModel;
           this.state = state;
           this.ownerId = ownerId;
           this.cDate = cDate;
           this.mDate = mDate;
           this.dcmDate = dcmDate;
           this.bDef = bDef;
           this.bMech = bMech;
           this.title = title;
           this.creator = creator;
           this.subject = subject;
           this.description = description;
           this.publisher = publisher;
           this.contributor = contributor;
           this.date = date;
           this.type = type;
           this.format = format;
           this.identifier = identifier;
           this.source = source;
           this.language = language;
           this.relation = relation;
           this.coverage = coverage;
           this.rights = rights;
    }


    /**
     * Gets the pid value for this ObjectFields.
     * 
     * @return pid
     */
    public java.lang.String getPid() {
        return pid;
    }


    /**
     * Sets the pid value for this ObjectFields.
     * 
     * @param pid
     */
    public void setPid(java.lang.String pid) {
        this.pid = pid;
    }


    /**
     * Gets the label value for this ObjectFields.
     * 
     * @return label
     */
    public java.lang.String getLabel() {
        return label;
    }


    /**
     * Sets the label value for this ObjectFields.
     * 
     * @param label
     */
    public void setLabel(java.lang.String label) {
        this.label = label;
    }


    /**
     * Gets the fType value for this ObjectFields.
     * 
     * @return fType
     */
    public java.lang.String getFType() {
        return fType;
    }


    /**
     * Sets the fType value for this ObjectFields.
     * 
     * @param fType
     */
    public void setFType(java.lang.String fType) {
        this.fType = fType;
    }


    /**
     * Gets the cModel value for this ObjectFields.
     * 
     * @return cModel
     */
    public java.lang.String getCModel() {
        return cModel;
    }


    /**
     * Sets the cModel value for this ObjectFields.
     * 
     * @param cModel
     */
    public void setCModel(java.lang.String cModel) {
        this.cModel = cModel;
    }


    /**
     * Gets the state value for this ObjectFields.
     * 
     * @return state
     */
    public java.lang.String getState() {
        return state;
    }


    /**
     * Sets the state value for this ObjectFields.
     * 
     * @param state
     */
    public void setState(java.lang.String state) {
        this.state = state;
    }


    /**
     * Gets the ownerId value for this ObjectFields.
     * 
     * @return ownerId
     */
    public java.lang.String getOwnerId() {
        return ownerId;
    }


    /**
     * Sets the ownerId value for this ObjectFields.
     * 
     * @param ownerId
     */
    public void setOwnerId(java.lang.String ownerId) {
        this.ownerId = ownerId;
    }


    /**
     * Gets the cDate value for this ObjectFields.
     * 
     * @return cDate
     */
    public java.lang.String getCDate() {
        return cDate;
    }


    /**
     * Sets the cDate value for this ObjectFields.
     * 
     * @param cDate
     */
    public void setCDate(java.lang.String cDate) {
        this.cDate = cDate;
    }


    /**
     * Gets the mDate value for this ObjectFields.
     * 
     * @return mDate
     */
    public java.lang.String getMDate() {
        return mDate;
    }


    /**
     * Sets the mDate value for this ObjectFields.
     * 
     * @param mDate
     */
    public void setMDate(java.lang.String mDate) {
        this.mDate = mDate;
    }


    /**
     * Gets the dcmDate value for this ObjectFields.
     * 
     * @return dcmDate
     */
    public java.lang.String getDcmDate() {
        return dcmDate;
    }


    /**
     * Sets the dcmDate value for this ObjectFields.
     * 
     * @param dcmDate
     */
    public void setDcmDate(java.lang.String dcmDate) {
        this.dcmDate = dcmDate;
    }


    /**
     * Gets the bDef value for this ObjectFields.
     * 
     * @return bDef
     */
    public java.lang.String[] getBDef() {
        return bDef;
    }


    /**
     * Sets the bDef value for this ObjectFields.
     * 
     * @param bDef
     */
    public void setBDef(java.lang.String[] bDef) {
        this.bDef = bDef;
    }

    public java.lang.String getBDef(int i) {
        return this.bDef[i];
    }

    public void setBDef(int i, java.lang.String _value) {
        this.bDef[i] = _value;
    }


    /**
     * Gets the bMech value for this ObjectFields.
     * 
     * @return bMech
     */
    public java.lang.String[] getBMech() {
        return bMech;
    }


    /**
     * Sets the bMech value for this ObjectFields.
     * 
     * @param bMech
     */
    public void setBMech(java.lang.String[] bMech) {
        this.bMech = bMech;
    }

    public java.lang.String getBMech(int i) {
        return this.bMech[i];
    }

    public void setBMech(int i, java.lang.String _value) {
        this.bMech[i] = _value;
    }


    /**
     * Gets the title value for this ObjectFields.
     * 
     * @return title
     */
    public java.lang.String[] getTitle() {
        return title;
    }


    /**
     * Sets the title value for this ObjectFields.
     * 
     * @param title
     */
    public void setTitle(java.lang.String[] title) {
        this.title = title;
    }

    public java.lang.String getTitle(int i) {
        return this.title[i];
    }

    public void setTitle(int i, java.lang.String _value) {
        this.title[i] = _value;
    }


    /**
     * Gets the creator value for this ObjectFields.
     * 
     * @return creator
     */
    public java.lang.String[] getCreator() {
        return creator;
    }


    /**
     * Sets the creator value for this ObjectFields.
     * 
     * @param creator
     */
    public void setCreator(java.lang.String[] creator) {
        this.creator = creator;
    }

    public java.lang.String getCreator(int i) {
        return this.creator[i];
    }

    public void setCreator(int i, java.lang.String _value) {
        this.creator[i] = _value;
    }


    /**
     * Gets the subject value for this ObjectFields.
     * 
     * @return subject
     */
    public java.lang.String[] getSubject() {
        return subject;
    }


    /**
     * Sets the subject value for this ObjectFields.
     * 
     * @param subject
     */
    public void setSubject(java.lang.String[] subject) {
        this.subject = subject;
    }

    public java.lang.String getSubject(int i) {
        return this.subject[i];
    }

    public void setSubject(int i, java.lang.String _value) {
        this.subject[i] = _value;
    }


    /**
     * Gets the description value for this ObjectFields.
     * 
     * @return description
     */
    public java.lang.String[] getDescription() {
        return description;
    }


    /**
     * Sets the description value for this ObjectFields.
     * 
     * @param description
     */
    public void setDescription(java.lang.String[] description) {
        this.description = description;
    }

    public java.lang.String getDescription(int i) {
        return this.description[i];
    }

    public void setDescription(int i, java.lang.String _value) {
        this.description[i] = _value;
    }


    /**
     * Gets the publisher value for this ObjectFields.
     * 
     * @return publisher
     */
    public java.lang.String[] getPublisher() {
        return publisher;
    }


    /**
     * Sets the publisher value for this ObjectFields.
     * 
     * @param publisher
     */
    public void setPublisher(java.lang.String[] publisher) {
        this.publisher = publisher;
    }

    public java.lang.String getPublisher(int i) {
        return this.publisher[i];
    }

    public void setPublisher(int i, java.lang.String _value) {
        this.publisher[i] = _value;
    }


    /**
     * Gets the contributor value for this ObjectFields.
     * 
     * @return contributor
     */
    public java.lang.String[] getContributor() {
        return contributor;
    }


    /**
     * Sets the contributor value for this ObjectFields.
     * 
     * @param contributor
     */
    public void setContributor(java.lang.String[] contributor) {
        this.contributor = contributor;
    }

    public java.lang.String getContributor(int i) {
        return this.contributor[i];
    }

    public void setContributor(int i, java.lang.String _value) {
        this.contributor[i] = _value;
    }


    /**
     * Gets the date value for this ObjectFields.
     * 
     * @return date
     */
    public java.lang.String[] getDate() {
        return date;
    }


    /**
     * Sets the date value for this ObjectFields.
     * 
     * @param date
     */
    public void setDate(java.lang.String[] date) {
        this.date = date;
    }

    public java.lang.String getDate(int i) {
        return this.date[i];
    }

    public void setDate(int i, java.lang.String _value) {
        this.date[i] = _value;
    }


    /**
     * Gets the type value for this ObjectFields.
     * 
     * @return type
     */
    public java.lang.String[] getType() {
        return type;
    }


    /**
     * Sets the type value for this ObjectFields.
     * 
     * @param type
     */
    public void setType(java.lang.String[] type) {
        this.type = type;
    }

    public java.lang.String getType(int i) {
        return this.type[i];
    }

    public void setType(int i, java.lang.String _value) {
        this.type[i] = _value;
    }


    /**
     * Gets the format value for this ObjectFields.
     * 
     * @return format
     */
    public java.lang.String[] getFormat() {
        return format;
    }


    /**
     * Sets the format value for this ObjectFields.
     * 
     * @param format
     */
    public void setFormat(java.lang.String[] format) {
        this.format = format;
    }

    public java.lang.String getFormat(int i) {
        return this.format[i];
    }

    public void setFormat(int i, java.lang.String _value) {
        this.format[i] = _value;
    }


    /**
     * Gets the identifier value for this ObjectFields.
     * 
     * @return identifier
     */
    public java.lang.String[] getIdentifier() {
        return identifier;
    }


    /**
     * Sets the identifier value for this ObjectFields.
     * 
     * @param identifier
     */
    public void setIdentifier(java.lang.String[] identifier) {
        this.identifier = identifier;
    }

    public java.lang.String getIdentifier(int i) {
        return this.identifier[i];
    }

    public void setIdentifier(int i, java.lang.String _value) {
        this.identifier[i] = _value;
    }


    /**
     * Gets the source value for this ObjectFields.
     * 
     * @return source
     */
    public java.lang.String[] getSource() {
        return source;
    }


    /**
     * Sets the source value for this ObjectFields.
     * 
     * @param source
     */
    public void setSource(java.lang.String[] source) {
        this.source = source;
    }

    public java.lang.String getSource(int i) {
        return this.source[i];
    }

    public void setSource(int i, java.lang.String _value) {
        this.source[i] = _value;
    }


    /**
     * Gets the language value for this ObjectFields.
     * 
     * @return language
     */
    public java.lang.String[] getLanguage() {
        return language;
    }


    /**
     * Sets the language value for this ObjectFields.
     * 
     * @param language
     */
    public void setLanguage(java.lang.String[] language) {
        this.language = language;
    }

    public java.lang.String getLanguage(int i) {
        return this.language[i];
    }

    public void setLanguage(int i, java.lang.String _value) {
        this.language[i] = _value;
    }


    /**
     * Gets the relation value for this ObjectFields.
     * 
     * @return relation
     */
    public java.lang.String[] getRelation() {
        return relation;
    }


    /**
     * Sets the relation value for this ObjectFields.
     * 
     * @param relation
     */
    public void setRelation(java.lang.String[] relation) {
        this.relation = relation;
    }

    public java.lang.String getRelation(int i) {
        return this.relation[i];
    }

    public void setRelation(int i, java.lang.String _value) {
        this.relation[i] = _value;
    }


    /**
     * Gets the coverage value for this ObjectFields.
     * 
     * @return coverage
     */
    public java.lang.String[] getCoverage() {
        return coverage;
    }


    /**
     * Sets the coverage value for this ObjectFields.
     * 
     * @param coverage
     */
    public void setCoverage(java.lang.String[] coverage) {
        this.coverage = coverage;
    }

    public java.lang.String getCoverage(int i) {
        return this.coverage[i];
    }

    public void setCoverage(int i, java.lang.String _value) {
        this.coverage[i] = _value;
    }


    /**
     * Gets the rights value for this ObjectFields.
     * 
     * @return rights
     */
    public java.lang.String[] getRights() {
        return rights;
    }


    /**
     * Sets the rights value for this ObjectFields.
     * 
     * @param rights
     */
    public void setRights(java.lang.String[] rights) {
        this.rights = rights;
    }

    public java.lang.String getRights(int i) {
        return this.rights[i];
    }

    public void setRights(int i, java.lang.String _value) {
        this.rights[i] = _value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ObjectFields)) return false;
        ObjectFields other = (ObjectFields) obj;
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
            ((this.label==null && other.getLabel()==null) || 
             (this.label!=null &&
              this.label.equals(other.getLabel()))) &&
            ((this.fType==null && other.getFType()==null) || 
             (this.fType!=null &&
              this.fType.equals(other.getFType()))) &&
            ((this.cModel==null && other.getCModel()==null) || 
             (this.cModel!=null &&
              this.cModel.equals(other.getCModel()))) &&
            ((this.state==null && other.getState()==null) || 
             (this.state!=null &&
              this.state.equals(other.getState()))) &&
            ((this.ownerId==null && other.getOwnerId()==null) || 
             (this.ownerId!=null &&
              this.ownerId.equals(other.getOwnerId()))) &&
            ((this.cDate==null && other.getCDate()==null) || 
             (this.cDate!=null &&
              this.cDate.equals(other.getCDate()))) &&
            ((this.mDate==null && other.getMDate()==null) || 
             (this.mDate!=null &&
              this.mDate.equals(other.getMDate()))) &&
            ((this.dcmDate==null && other.getDcmDate()==null) || 
             (this.dcmDate!=null &&
              this.dcmDate.equals(other.getDcmDate()))) &&
            ((this.bDef==null && other.getBDef()==null) || 
             (this.bDef!=null &&
              java.util.Arrays.equals(this.bDef, other.getBDef()))) &&
            ((this.bMech==null && other.getBMech()==null) || 
             (this.bMech!=null &&
              java.util.Arrays.equals(this.bMech, other.getBMech()))) &&
            ((this.title==null && other.getTitle()==null) || 
             (this.title!=null &&
              java.util.Arrays.equals(this.title, other.getTitle()))) &&
            ((this.creator==null && other.getCreator()==null) || 
             (this.creator!=null &&
              java.util.Arrays.equals(this.creator, other.getCreator()))) &&
            ((this.subject==null && other.getSubject()==null) || 
             (this.subject!=null &&
              java.util.Arrays.equals(this.subject, other.getSubject()))) &&
            ((this.description==null && other.getDescription()==null) || 
             (this.description!=null &&
              java.util.Arrays.equals(this.description, other.getDescription()))) &&
            ((this.publisher==null && other.getPublisher()==null) || 
             (this.publisher!=null &&
              java.util.Arrays.equals(this.publisher, other.getPublisher()))) &&
            ((this.contributor==null && other.getContributor()==null) || 
             (this.contributor!=null &&
              java.util.Arrays.equals(this.contributor, other.getContributor()))) &&
            ((this.date==null && other.getDate()==null) || 
             (this.date!=null &&
              java.util.Arrays.equals(this.date, other.getDate()))) &&
            ((this.type==null && other.getType()==null) || 
             (this.type!=null &&
              java.util.Arrays.equals(this.type, other.getType()))) &&
            ((this.format==null && other.getFormat()==null) || 
             (this.format!=null &&
              java.util.Arrays.equals(this.format, other.getFormat()))) &&
            ((this.identifier==null && other.getIdentifier()==null) || 
             (this.identifier!=null &&
              java.util.Arrays.equals(this.identifier, other.getIdentifier()))) &&
            ((this.source==null && other.getSource()==null) || 
             (this.source!=null &&
              java.util.Arrays.equals(this.source, other.getSource()))) &&
            ((this.language==null && other.getLanguage()==null) || 
             (this.language!=null &&
              java.util.Arrays.equals(this.language, other.getLanguage()))) &&
            ((this.relation==null && other.getRelation()==null) || 
             (this.relation!=null &&
              java.util.Arrays.equals(this.relation, other.getRelation()))) &&
            ((this.coverage==null && other.getCoverage()==null) || 
             (this.coverage!=null &&
              java.util.Arrays.equals(this.coverage, other.getCoverage()))) &&
            ((this.rights==null && other.getRights()==null) || 
             (this.rights!=null &&
              java.util.Arrays.equals(this.rights, other.getRights())));
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
        if (getLabel() != null) {
            _hashCode += getLabel().hashCode();
        }
        if (getFType() != null) {
            _hashCode += getFType().hashCode();
        }
        if (getCModel() != null) {
            _hashCode += getCModel().hashCode();
        }
        if (getState() != null) {
            _hashCode += getState().hashCode();
        }
        if (getOwnerId() != null) {
            _hashCode += getOwnerId().hashCode();
        }
        if (getCDate() != null) {
            _hashCode += getCDate().hashCode();
        }
        if (getMDate() != null) {
            _hashCode += getMDate().hashCode();
        }
        if (getDcmDate() != null) {
            _hashCode += getDcmDate().hashCode();
        }
        if (getBDef() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getBDef());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getBDef(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getBMech() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getBMech());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getBMech(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getTitle() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getTitle());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getTitle(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getCreator() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getCreator());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getCreator(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getSubject() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSubject());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSubject(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getDescription() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getDescription());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getDescription(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getPublisher() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getPublisher());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getPublisher(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getContributor() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getContributor());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getContributor(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getDate() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getDate());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getDate(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getType() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getType());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getType(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getFormat() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getFormat());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getFormat(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getIdentifier() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getIdentifier());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getIdentifier(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getSource() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSource());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSource(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getLanguage() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getLanguage());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getLanguage(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getRelation() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getRelation());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getRelation(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getCoverage() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getCoverage());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getCoverage(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getRights() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getRights());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getRights(), i);
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
        new org.apache.axis.description.TypeDesc(ObjectFields.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.fedora.info/definitions/1/0/types/", "ObjectFields"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("pid");
        elemField.setXmlName(new javax.xml.namespace.QName("", "pid"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("label");
        elemField.setXmlName(new javax.xml.namespace.QName("", "label"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("FType");
        elemField.setXmlName(new javax.xml.namespace.QName("", "fType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("CModel");
        elemField.setXmlName(new javax.xml.namespace.QName("", "cModel"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("state");
        elemField.setXmlName(new javax.xml.namespace.QName("", "state"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ownerId");
        elemField.setXmlName(new javax.xml.namespace.QName("", "ownerId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("CDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "cDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("MDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "mDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dcmDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "dcmDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("BDef");
        elemField.setXmlName(new javax.xml.namespace.QName("", "bDef"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("BMech");
        elemField.setXmlName(new javax.xml.namespace.QName("", "bMech"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("title");
        elemField.setXmlName(new javax.xml.namespace.QName("", "title"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("creator");
        elemField.setXmlName(new javax.xml.namespace.QName("", "creator"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("subject");
        elemField.setXmlName(new javax.xml.namespace.QName("", "subject"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("description");
        elemField.setXmlName(new javax.xml.namespace.QName("", "description"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("publisher");
        elemField.setXmlName(new javax.xml.namespace.QName("", "publisher"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("contributor");
        elemField.setXmlName(new javax.xml.namespace.QName("", "contributor"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("date");
        elemField.setXmlName(new javax.xml.namespace.QName("", "date"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("type");
        elemField.setXmlName(new javax.xml.namespace.QName("", "type"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("format");
        elemField.setXmlName(new javax.xml.namespace.QName("", "format"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("identifier");
        elemField.setXmlName(new javax.xml.namespace.QName("", "identifier"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("source");
        elemField.setXmlName(new javax.xml.namespace.QName("", "source"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("language");
        elemField.setXmlName(new javax.xml.namespace.QName("", "language"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("relation");
        elemField.setXmlName(new javax.xml.namespace.QName("", "relation"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("coverage");
        elemField.setXmlName(new javax.xml.namespace.QName("", "coverage"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("rights");
        elemField.setXmlName(new javax.xml.namespace.QName("", "rights"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(true);
        elemField.setMaxOccursUnbounded(true);
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
