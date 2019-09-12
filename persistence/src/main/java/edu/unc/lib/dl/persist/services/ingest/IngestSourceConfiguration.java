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
package edu.unc.lib.dl.persist.services.ingest;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import edu.unc.lib.dl.fedora.PID;

/**
 * Configuration for a ingest source
 *
 * @author bbpennel
 * @date Oct 22, 2015
 */
public class IngestSourceConfiguration {

    private String id;
    private String name;
    private String base;
    private List<String> patterns;
    private List<String> containers;

    public IngestSourceConfiguration() {
    }

    public IngestSourceConfiguration(String id, String name, String base, List<String> patterns,
            List<PID> containers) {
        this.id = id;
        this.name = name;
        this.base = base;
        this.patterns = patterns;
        this.containers = containers.stream().map(PID::getId).collect(Collectors.toList());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = Paths.get(base).toAbsolutePath().toString();
    }

    public List<String> getContainers() {
        return containers;
    }

    public void setContainers(List<String> containers) {
        this.containers = containers;
    }

}
