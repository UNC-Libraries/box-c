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
    private List<Keywords> keywords;
    private String language;
    private String resourceType;
    // Continuing resource fields
    private String dateOfIssue;
    private String volume;
    private String number;
    private String alternateTitle;
    private String precedingTitle;
    private String succeedingTitle;
    private String genre;
    private String publisher;
    private String placeOfPublication;
    private String issuance;
    private String frequency;
    private String relatedUrl;

    private List<CreatorInfo> creatorInfo;
    private List<CorporateCreator> corporateCreator;
    private List<SubjectTopicalEntry> subjectTopical;
    private List<SubjectPersonalEntry> subjectPersonal;
    private List<SubjectCorporateEntry> subjectCorporate;
    private List<SubjectGeographicEntry> subjectGeographic;
    private List<FileInfo> file;

    // Default constructor required for Jackson
    public WorkFormData() {
        this.keywords = new ArrayList<>();
        this.creatorInfo = new ArrayList<>();
        this.corporateCreator = new ArrayList<>();
        this.subjectTopical = new ArrayList<>();
        this.subjectPersonal = new ArrayList<>();
        this.subjectCorporate = new ArrayList<>();
        this.subjectGeographic = new ArrayList<>();
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

    public List<Keywords> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<Keywords> keywords) {
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

    public List<CreatorInfo> getCreatorInfo() {
        return creatorInfo;
    }

    public void setCreatorInfo(List<CreatorInfo> creatorInfo) {
        this.creatorInfo = creatorInfo;
    }

    public String getDateOfIssue() {
        return dateOfIssue;
    }

    public void setDateOfIssue(String dateOfIssue) {
        this.dateOfIssue = dateOfIssue;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getAlternateTitle() {
        return alternateTitle;
    }

    public void setAlternateTitle(String alternateTitle) {
        this.alternateTitle = alternateTitle;
    }

    public String getPrecedingTitle() {
        return precedingTitle;
    }

    public void setPrecedingTitle(String precedingTitle) {
        this.precedingTitle = precedingTitle;
    }

    public String getSucceedingTitle() {
        return succeedingTitle;
    }

    public void setSucceedingTitle(String succeedingTitle) {
        this.succeedingTitle = succeedingTitle;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPlaceOfPublication() {
        return placeOfPublication;
    }

    public void setPlaceOfPublication(String placeOfPublication) {
        this.placeOfPublication = placeOfPublication;
    }

    public String getIssuance() {
        return issuance;
    }

    public void setIssuance(String issuance) {
        this.issuance = issuance;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getRelatedUrl() {
        return relatedUrl;
    }

    public void setRelatedUrl(String relatedUrl) {
        this.relatedUrl = relatedUrl;
    }

    public List<CorporateCreator> getCorporateCreator() {
        return corporateCreator;
    }

    public void setCorporateCreator(List<CorporateCreator> corporateCreator) {
        this.corporateCreator = corporateCreator;
    }

    public List<SubjectTopicalEntry> getSubjectTopical() {
        return subjectTopical;
    }

    public void setSubjectTopical(List<SubjectTopicalEntry> subjectTopical) {
        this.subjectTopical = subjectTopical;
    }

    public List<SubjectPersonalEntry> getSubjectPersonal() {
        return subjectPersonal;
    }

    public void setSubjectPersonal(List<SubjectPersonalEntry> subjectPersonal) {
        this.subjectPersonal = subjectPersonal;
    }

    public List<SubjectCorporateEntry> getSubjectCorporate() {
        return subjectCorporate;
    }

    public void setSubjectCorporate(List<SubjectCorporateEntry> subjectCorporate) {
        this.subjectCorporate = subjectCorporate;
    }

    public List<SubjectGeographicEntry> getSubjectGeographic() {
        return subjectGeographic;
    }

    public void setSubjectGeographic(List<SubjectGeographicEntry> subjectGeographic) {
        this.subjectGeographic = subjectGeographic;
    }

    public List<FileInfo> getFile() {
        return file;
    }

    @JsonProperty(required = true)
    public void setFile(List<FileInfo> file) {
        this.file = file;
    }

    public static class CreatorInfo {
        private String fname;
        private String lname;
        private String termsAddress;
        private String dates;

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
    }

    public static class CorporateCreator {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Keywords {
        private String keyword;

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
    }

    public static class SubjectTopicalEntry {
        private String subjectTopical;

        public String getSubjectTopical() {
            return subjectTopical;
        }

        public void setSubjectTopical(String subjectTopical) {
            this.subjectTopical = subjectTopical;
        }
    }

    public static class SubjectPersonalEntry {
        private String subjectPersonal;

        public String getSubjectPersonal() {
            return subjectPersonal;
        }

        public void setSubjectPersonal(String subjectPersonal) {
            this.subjectPersonal = subjectPersonal;
        }
    }

    public static class SubjectCorporateEntry {
        private String subjectCorporate;

        public String getSubjectCorporate() {
            return subjectCorporate;
        }

        public void setSubjectCorporate(String subjectCorporate) {
            this.subjectCorporate = subjectCorporate;
        }
    }

    public static class SubjectGeographicEntry {
        private String subjectGeographic;

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
