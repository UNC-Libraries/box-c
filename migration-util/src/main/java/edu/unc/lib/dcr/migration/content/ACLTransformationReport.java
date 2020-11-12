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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks and reports on details of access control transformations
 *
 * @author bbpennel
 */
public class ACLTransformationReport {
    public static AtomicInteger hasEmbargo = new AtomicInteger();
    public static AtomicInteger isUnpublished = new AtomicInteger();
    public static AtomicInteger hasInvalidPatronGroup = new AtomicInteger();

    private ACLTransformationReport() {
    }

    /**
     * @return formatted report of the results
     */
    public static String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("ACL Transformation Report:\n")
            .append("  Invalid patron groups: ").append(hasInvalidPatronGroup.get()).append('\n')
            .append("  Unpublished: ").append(isUnpublished.get()).append('\n')
            .append("  Embargoed: ").append(hasEmbargo.get());
        return sb.toString();
    }
}
