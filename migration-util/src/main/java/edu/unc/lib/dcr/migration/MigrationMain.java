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

import java.util.concurrent.Callable;

import org.slf4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Main class for the migration tool
 *
 * @author bbpennel
 *
 */
@Command(subcommands = {
        TransformPremis.class
    })
public class MigrationMain implements Callable<Integer> {
    private static final Logger output = getLogger(OUTPUT_LOGGER);

    private MigrationMain() {
    }

    @Override
    public Integer call() throws Exception {
        output.info(BannerUtility.getBanner());
        return 0;
    }

    @Command(name = "chomp")
    public int chomp() {
        output.info(BannerUtility.getErrorBanner());
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MigrationMain()).execute(args);
        System.exit(exitCode);
    }



}
