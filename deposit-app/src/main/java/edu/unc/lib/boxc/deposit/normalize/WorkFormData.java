package edu.unc.lib.boxc.deposit.normalize;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bbpennel
 */
public class WorkFormData {
    private String title;
    private String dateCreated;
    private String description;
    private List<String> keywords;
    private String language;
    private String resourceType;
    private String fname;
    private String lname;
    private String termsAddress;
    private String dates;
    private List<CorporateCreator> corporateCreator;
    private List<SubjectTopical> subjectInfoTopical;
    private List<SubjectPersonalName> subjectPersonalName;
    private List<SubjectCorporateName> subjectInfoCorporateName;
    private List<SubjectGeographic> subjectInfoGeographic;
    private List<FileInfo> file;
    private String depositorEmail;

    // Default constructor required for Jackson
    public WorkFormData() {
        this.keywords = new ArrayList<>();
        this.corporateCreator = new ArrayList<>();
        this.subjectInfoTopical = new ArrayList<>();
        this.subjectPersonalName = new ArrayList<>();
        this.subjectInfoCorporateName = new ArrayList<>();
        this.subjectInfoGeographic = new ArrayList<>();
        this.file = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    @JsonProperty(required = true)
    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public String getTermsAddress() {
        return termsAddress;
    }

    public void setTermsAddress(String termsAddress) {
        this.termsAddress = termsAddress;
    }

    public String getDates() {
        return dates;
    }

    public void setDates(String dates) {
        this.dates = dates;
    }

    public List<CorporateCreator> getCorporateCreator() {
        return corporateCreator;
    }

    public void setCorporateCreator(List<CorporateCreator> corporateCreator) {
        this.corporateCreator = corporateCreator;
    }

    public List<SubjectTopical> getSubjectInfoTopical() {
        return subjectInfoTopical;
    }

    public void setSubjectInfoTopical(List<SubjectTopical> subjectInfoTopical) {
        this.subjectInfoTopical = subjectInfoTopical;
    }

    public List<SubjectPersonalName> getSubjectPersonalName() {
        return subjectPersonalName;
    }

    public void setSubjectPersonalName(List<SubjectPersonalName> subjectPersonalName) {
        this.subjectPersonalName = subjectPersonalName;
    }

    public List<SubjectCorporateName> getSubjectInfoCorporateName() {
        return subjectInfoCorporateName;
    }

    public void setSubjectInfoCorporateName(List<SubjectCorporateName> subjectInfoCorporateName) {
        this.subjectInfoCorporateName = subjectInfoCorporateName;
    }

    public List<SubjectGeographic> getSubjectInfoGeographic() {
        return subjectInfoGeographic;
    }

    public void setSubjectInfoGeographic(List<SubjectGeographic> subjectInfoGeographic) {
        this.subjectInfoGeographic = subjectInfoGeographic;
    }

    public List<FileInfo> getFile() {
        return file;
    }

    @JsonProperty(required = true)
    public void setFile(List<FileInfo> file) {
        this.file = file;
    }

    public String getDepositorEmail() {
        return depositorEmail;
    }

    public void setDepositorEmail(String depositorEmail) {
        this.depositorEmail = depositorEmail;
    }
    public static class CorporateCreator {
        private String name;

        public CorporateCreator() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class SubjectTopical {
        private String subjectTopical;

        public SubjectTopical() {
        }

        public String getSubjectTopical() {
            return subjectTopical;
        }

        public void setSubjectTopical(String subjectTopical) {
            this.subjectTopical = subjectTopical;
        }
    }

    public static class SubjectPersonalName {
        private String subjectPersonalName;

        public SubjectPersonalName() {
        }

        public String getSubjectPersonalName() {
            return subjectPersonalName;
        }

        public void setSubjectPersonalName(String subjectPersonalName) {
            this.subjectPersonalName = subjectPersonalName;
        }
    }

    public static class SubjectCorporateName {
        private String subjectCorporateName;

        public SubjectCorporateName() {
        }

        public String getSubjectCorporateName() {
            return subjectCorporateName;
        }

        public void setSubjectCorporateName(String subjectCorporateName) {
            this.subjectCorporateName = subjectCorporateName;
        }
    }

    public static class SubjectGeographic {
        private String subjectGeographic;

        public SubjectGeographic() {
        }

        public String getSubjectGeographic() {
            return subjectGeographic;
        }

        public void setSubjectGeographic(String subjectGeographic) {
            this.subjectGeographic = subjectGeographic;
        }
    }

    public static class FileInfo {
        private String originalName;
        private String tmp;

        public FileInfo() {
        }

        public String getOriginalName() {
            return originalName;
        }

        public void setOriginalName(String originalName) {
            this.originalName = originalName;
        }

        public String getTmp() {
            return tmp;
        }

        public void setTmp(String tmp) {
            this.tmp = tmp;
        }
    }
}
