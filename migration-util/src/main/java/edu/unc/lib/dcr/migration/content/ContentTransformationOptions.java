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
package edu.unc.lib.dcr.migration.content;

import edu.unc.lib.dl.fedora.PID;
import picocli.CommandLine.Option;

/**
 * Configuration options for a content transformation operation
 *
 * @author bbpennel
 */
public class ContentTransformationOptions {

    @Option(names = {"-u", "--as-admin-units"},
            description = "Top level collections will be transformed into admin units")
    private boolean topLevelAsUnit;

    @Option(names = {"--no-hash-nesting"}, negatable = true,
            description = "Nest transformed logs in hashed subdirectories. Default: true")
    private boolean hashNesting = true;

    @Option(names = {"-g", "--generate-ids"},
            description = "Generate new ids for transformed objects, for testing.")
    private boolean generateIds;

    @Option(names = {"-m", "--missing-deposit-records"},
            description = "Create referenced deposit records which do not have their own bxc3 object")
    private boolean createMissingDepositRecords;

    @Option(names = {"--skip-members"},
            description = "Only migrate the specified object, do not migrate its members")
    private boolean skipMembers;

    @Option(names = {"-n", "--dry-run"},
            description = "Perform the transformation but do not save the results")
    private boolean dryRun;

    @Option(names = {"--deposit-into"},
            description = "Submits the transformed content for deposit to the provided container UUID")
    private String depositInto;

    @Option(names = {"--default-storage-location"},
            defaultValue = "primary_storage",
            description = "Storage location that objects generated in fedora will stored binaries to")
    private String defaultStorageLocation;

    private PID depositPid;

    public boolean isTopLevelAsUnit() {
        return topLevelAsUnit;
    }

    public void setTopLevelAsUnit(boolean topLevelAsUnit) {
        this.topLevelAsUnit = topLevelAsUnit;
    }

    public boolean isHashNesting() {
        return hashNesting;
    }

    public void setHashNesting(boolean hashNesting) {
        this.hashNesting = hashNesting;
    }

    public boolean isGenerateIds() {
        return generateIds;
    }

    public void setGenerateIds(boolean generateIds) {
        this.generateIds = generateIds;
    }

    public boolean isCreateMissingDepositRecords() {
        return createMissingDepositRecords;
    }

    public void setCreateMissingDepositRecords(boolean createMissingDepositRecords) {
        this.createMissingDepositRecords = createMissingDepositRecords;
    }

    public boolean isSkipMembers() {
        return skipMembers;
    }

    public void setSkipMembers(boolean skipMembers) {
        this.skipMembers = skipMembers;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getDepositInto() {
        return depositInto;
    }

    public void setDepositInto(String depositInto) {
        this.depositInto = depositInto;
    }

    public PID getDepositPid() {
        return depositPid;
    }

    public void setDepositPid(PID depositPid) {
        this.depositPid = depositPid;
    }

    public String getDefaultStorageLocation() {
        return defaultStorageLocation;
    }

    public void setDefaultStorageLocation(String defaultStorageLocation) {
        this.defaultStorageLocation = defaultStorageLocation;
    }
}
