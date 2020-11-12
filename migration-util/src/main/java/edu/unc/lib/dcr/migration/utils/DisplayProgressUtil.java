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
package edu.unc.lib.dcr.migration.utils;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.repeat;

/**
 * Utilities for displaying progress in a CLI
 *
 * @author bbpennel
 */
public class DisplayProgressUtil {
    private static final int PROGRESS_BAR_UNITS = 40;
    private static final double PROGRESS_BAR_DIVIDOR = (double) 100 / PROGRESS_BAR_UNITS;

    /**
     * Render a progress bar, percent, and total
     *
     * @param current
     * @param total
     */
    public static void displayProgress(long current, long total) {
        long percent = Math.round(((float) current / total) * 100);
        int progressBars = (int) Math.round(percent / PROGRESS_BAR_DIVIDOR);

        StringBuilder sb = new StringBuilder("\r");
        sb.append(format("%1$3s", percent)).append("% [");
        sb.append(repeat("=", progressBars));
        sb.append(repeat(" ", PROGRESS_BAR_UNITS - progressBars));
        sb.append("] ").append(current).append("/").append(total);
        // Append spaces to clear rest of line
        sb.append(repeat(" ", 40));
        sb.append("\r");

        System.out.print(sb.toString());
        System.out.flush();
    }

    public static void finishProgress() {
        System.out.println();
        System.out.flush();
    }

    private DisplayProgressUtil() {
    }
}
