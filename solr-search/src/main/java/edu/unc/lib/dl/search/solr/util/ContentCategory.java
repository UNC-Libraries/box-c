/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.search.solr.util;

/**
 * 
 * @author bbpennel
 *
 */
public enum ContentCategory {
    dataset("Dataset"), image("Image"), diskimage("Disk Image"), video("Video"), software("Software"),
    audio("Audio"), archive("Archive File"), text("Text"), unknown("Unknown");

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