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
package edu.unc.lib.dl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;

import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.RDFXMLUtil;

/**
 * @author bbpennel
 * @date Aug 27, 2015
 */
public class ContainerSettings {

    private String title;
    private String defaultView;
    private List<String> views;

    public ContainerSettings(Element foxml) {
        Element relsEl = FOXMLJDOMUtil.getDatastreamContent(
                Datastream.RELS_EXT, foxml);
        setViews(RDFXMLUtil.getLiteralValues(relsEl,
                CDRProperty.collectionShowView.getPredicate(),
                CDRProperty.collectionShowView.getNamespace()));

        defaultView = RDFXMLUtil.getLiteralValue(relsEl,
                CDRProperty.collectionDefaultView.getPredicate(),
                CDRProperty.collectionDefaultView.getNamespace());
    }

    public ContainerSettings(Map<String, List<String>> triples) {
        setViews(triples.get(CDRProperty.collectionShowView.toString()));
        List<String> defaultViewValues = triples
                .get(CDRProperty.collectionDefaultView.toString());
        defaultView = defaultViewValues != null ? defaultViewValues.get(0)
                : null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDefaultView() {
        return defaultView;
    }

    public void setDefaultView(String defaultView) {
        this.defaultView = defaultView;
    }

    public List<String> getViews() {
        return views;
    }

    public void setViews(List<String> views) {
        if (views == null) {
            this.views = null;
            return;
        }

        this.views = new ArrayList<>(views.size());
        for (ContainerView view : ContainerView.values()) {
            if (views.contains(view.name())) {
                this.views.add(view.name());
            }
        }
    }

    public String getViewDisplayName(String view) {
        ContainerView viewEnum = ContainerView.valueOf(view);
        return viewEnum == null ? null : viewEnum.getDisplayName();
    }

    public Map<String, Map<String, Object>> getViewInfo() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        for (ContainerView view : ContainerView.values()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("displayName", view.getDisplayName());
            entry.put("description", view.getDescription());
            entry.put("required", new Boolean(view.isRequired()));

            result.put(view.name(), entry);
        }

        return result;
    }

    public static enum ContainerView {
        // Order of these values determines the order in which tabs appear, so
        // rearrange with care
        STRUCTURE("Structure",
                "A tree view of the hierachical structure of the collection"), LIST_CONTENTS(
                "List Contents",
                "A result view of files within this collection with hierarchy flattened"), DEPARTMENTS(
                "Departments",
                "A list of the departments associated with objects in this collection"), DESCRIPTION(
                "Description",
                "An overview of the contents of the collection and descriptive metadata"), EXPORTS(
                "Metadata",
                "Export options for data associated with this collection.",
                true);

        String displayName;
        String description;
        boolean required;

        private ContainerView(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
            this.required = false;
        }

        private ContainerView(String displayName, String description,
                boolean required) {
            this.displayName = displayName;
            this.description = description;
            this.required = required;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public boolean isRequired() {
            return required;
        }
    }
}
