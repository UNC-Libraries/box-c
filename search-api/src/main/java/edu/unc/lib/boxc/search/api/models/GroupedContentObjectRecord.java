package edu.unc.lib.boxc.search.api.models;

import java.util.List;

/**
 * Record containing aggregated content object records
 *
 * @author bbpennel
 */
public interface GroupedContentObjectRecord extends ContentObjectRecord {

    /**
     * @return ContentObjectRecord to be used as the representative record in place of the aggregate
     */
    ContentObjectRecord getRepresentative();

    /**
     * @return All items contained in this grouping
     */
    List<ContentObjectRecord> getItems();

    /**
     * @return Total count of items in this grouping, whether or not they were retrieved
     */
    Long getItemCount();

    /**
     * @return identifier for this group
     */
    String getGroupId();
}