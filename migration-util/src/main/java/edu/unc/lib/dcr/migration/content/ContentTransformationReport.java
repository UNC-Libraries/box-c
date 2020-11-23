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

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.dl.util.ResourceType;

/**
 * Tracks and reports on details of a content transformation job
 *
 * @author bbpennel
 */
public class ContentTransformationReport {
    public static AtomicInteger noOriginalDatastream = new AtomicInteger();
    public static AtomicInteger noPremis = new AtomicInteger();
    public static AtomicInteger collectionToFolder = new AtomicInteger();
    public static AtomicInteger fileToWork = new AtomicInteger();
    public static AtomicInteger missingFoxml = new AtomicInteger();
    public static AtomicInteger skippedDeleted = new AtomicInteger();
    public static AtomicInteger generatedDepositRecords = new AtomicInteger();
    private static EnumMap<ResourceType, AtomicInteger> typeCounts = new EnumMap<>(ResourceType.class);

    private ContentTransformationReport() {
    }

    public static void reset() {
        noOriginalDatastream.set(0);
        noPremis.set(0);
        collectionToFolder.set(0);
        fileToWork.set(0);
        missingFoxml.set(0);
        skippedDeleted.set(0);
        generatedDepositRecords.set(0);
        typeCounts.clear();
    }

    /**
     * Record that an object was migrated
     * @param resourceType type of the object recorded
     */
    public static void recordObjectTransformed(Resource resourceType) {
        ResourceType type = ResourceType.getResourceTypeByUri(resourceType.getURI());
        AtomicInteger count = typeCounts.get(type);
        if (count == null) {
            count = new AtomicInteger();
            typeCounts.put(type, count);
        }
        count.incrementAndGet();
    }

    /**
     * @return formatted report of the results
     */
    public static String report() {
        StringBuilder sb = new StringBuilder();

        sb.append("Content Transformation Report:\n");
        typeCounts.forEach((type, count) -> {
            sb.append("  ").append(type.name()).append(": ").append(count.get()).append('\n');
        });

        sb.append("Content Issues Report:\n")
            .append("  Missing FOXML: ").append(missingFoxml.get()).append('\n')
            .append("  Deleted records: ").append(skippedDeleted.get()).append('\n')
            .append("  No original datastream: ").append(noOriginalDatastream.get()).append('\n')
            .append("  Files changed to works: ").append(fileToWork.get()).append('\n')
            .append("  Collections changed to folders: ").append(collectionToFolder.get()).append('\n')
            .append("  Generated deposit records: ").append(generatedDepositRecords.get()).append('\n')
            .append("  No PREMIS log: ").append(noPremis.get());

        return sb.toString();
    }
}
