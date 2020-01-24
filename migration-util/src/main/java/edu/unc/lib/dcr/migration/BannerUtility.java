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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;

/**
 * @author bbpennel
 *
 */
public class BannerUtility {
    private static final Logger log = getLogger(BannerUtility.class);

    private BannerUtility() {
    }

    public static String getBanner() {
        return loadFile("boxc_banner.txt");
    }

    public static String getErrorBanner() {
        return loadFile("chomp.txt");
    }

    private static String loadFile(String rescPath) {
        URL fileUrl = BannerUtility.class.getResource("/classes/" + rescPath);
        try {
            File file = new File(fileUrl.toURI());
            return readFileToString(file, UTF_8);
        } catch (IOException | URISyntaxException e) {
            log.error("Failed to load banner", e);
            return "";
        }
    }
}
