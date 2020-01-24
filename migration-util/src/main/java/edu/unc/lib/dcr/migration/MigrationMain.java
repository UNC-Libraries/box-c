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
package edu.unc.lib.dcr.migration;

import static edu.unc.lib.dcr.migration.MigrationConstants.OUTPUT_LOGGER;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * Main class for the migration tool
 *
 * @author bbpennel
 *
 */
public class MigrationMain {
    private static final Logger output = getLogger(OUTPUT_LOGGER);

    private MigrationMain() {
    }

    public static void main(String[] args) {
        // This will be replaced with a CLI framework later
        if (args.length > 0 && args[0].equals("chomp")) {
            output.info(BannerUtility.getErrorBanner());
            return;
        }

        output.info(BannerUtility.getBanner());
        output.info("Performing migration");
    }

}
