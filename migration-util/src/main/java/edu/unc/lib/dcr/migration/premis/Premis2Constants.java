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
package edu.unc.lib.dcr.migration.premis;

/**
 * @author bbpennel
 *
 */
public class Premis2Constants {

    private Premis2Constants() {
    }

    public static final String NORMALIZATION_TYPE = "http://id.loc.gov/vocabulary/preservationEvents/normalization";
    public static final String VALIDATION_TYPE = "http://id.loc.gov/vocabulary/preservationEvents/validation";
    public static final String VIRUS_CHECK_TYPE = "http://id.loc.gov/vocabulary/preservationEvents/virusCheck";
    public static final String INGESTION_TYPE = "http://id.loc.gov/vocabulary/preservationEvents/ingestion";
    public static final String REPLICATION_TYPE = "http://id.loc.gov/vocabulary/preservationEvents/replication";
    public static final String MIGRATION_TYPE = "http://id.loc.gov/vocabulary/preservationEvents/migration";
    public static final String DELETION_TYPE = "http://id.loc.gov/vocabulary/preservationEvents/deletion";
    public static final String FIXITY_CHECK_TYPE = "http://id.loc.gov/vocabulary/preservationEvents/fixityCheck";
    public static final String CREATION_TYPE = "http://id.loc.gov/vocabulary/preservationEvents/creation";

    public static final String VIRUS_AGENT = "edu.unc.lib.deposit.validate.VirusScanJob";
    public static final String INGEST_AGENT = "edu.unc.lib.deposit.fcrepo3.IngestDeposit";
    public static final String VALIDATE_MODS_AGENT = "edu.unc.lib.deposit.validate.ValidateMODS";
    public static final String METS_NORMAL_AGENT = "edu.unc.lib.deposit.normalize.CDRMETS2N3BagJob";
    public static final String FIXITY_AGENT = "edu.unc.lib.dl.cdr.services.fixity.FixityLogTask";

    public static final String SOFTWARE_ROLE = "Software";
    public static final String INITIATOR_ROLE = "Initiator";
}
