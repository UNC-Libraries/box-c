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
package edu.unc.lib.dl.search.solr.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author count0
 *
 */
public class Tag {

    /**
     * The tag label.
     */
    private String label = null;

    /**
     * List of details about the data described in the tag, such as roles or dates
     */
    private List<String> details;

    public Tag(String label) {
        this.label = label;
    }

    public Tag(String label, String detail) {
        this.label = label;
        this.addDetail(detail);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void addDetail(String detail) {
        if (details == null) {
            details = new ArrayList<String>();
        }
        details.add(detail);
    }

    public List<String> getDetails() {
        return this.details;
    }
}
