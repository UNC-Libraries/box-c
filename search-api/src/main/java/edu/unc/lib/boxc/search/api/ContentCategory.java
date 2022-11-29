package edu.unc.lib.boxc.search.api;

/**
 * 
 * @author bbpennel
 *
 */
public enum ContentCategory {
    image("Image"), diskimage("Disk Image"), video("Video"), software("Software"),
    spreadsheet("Spreadsheet"), audio("Audio"), archive("Archive File"),
    text("Text"), database("Database"), email("Email"), unknown("Unknown");

    private String displayName;
    private String joined;

    ContentCategory(String displayName) {
        this.displayName = displayName;
        this.joined = this.name() + "," + this.displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getJoined() {
        return joined;
    }

    public static ContentCategory getContentCategory(String name) {
        if (name == null) {
            return unknown;
        }
        for (ContentCategory category: values()) {
            if (category.name().equals(name)) {
                return category;
            }
        }
        return unknown;
    }
}