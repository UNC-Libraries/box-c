package edu.unc.lib.boxc.indexing.solr.utils;

/**
 * Info about Jp2 images
 *
 * @author bbpennel
 */
public class Jp2Info {
    private int width;
    private int height;

    public Jp2Info() {
    }

    public Jp2Info(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * @return the Jp2's info formatted as an extent string, which is heightxwidth, or an empty string if no dimensions
     */
    public String getExtent() {
        if (width == 0 || height == 0) {
            return "";
        }
        return height + "x" + width;
    }
}
