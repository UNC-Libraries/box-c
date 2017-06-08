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
package edu.unc.lib.dl.admin.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 
 * @author lfarrell
 *
 */
@Controller
public class PerformanceMonitorController {
    private final long MILLISECONDS_IN_ONE_HOUR = 60 * 60 * 1000;
    private static final Logger log = LoggerFactory
            .getLogger(PerformanceMonitorController.class);

    private static final String NEW_LINE_SEPARATOR = "\n";
    private static final Object [] FILE_HEADERS = {
        "date",
        "uuid",
        "throughput_files",
        "throughput_bytes",
        "queued_duration",
        "ingest_duration",
        "finished",
        "moves",
        "image_enh",
        "failed_image_enh",
        "metadata_enh",
        "failed_metadata_enh",
        "solr_enh",
        "failed_solr_enh",
        "fulltext_enh",
        "failed_fulltext_enh",
        "thumbnail_enh",
        "failed_thumbnail_enh",
        "failed_deposit",
        "failed_deposit_job"
    };

    private static final String[] MOVES_ENHANCEMENTS_JOBS_ARRAY = {
        "moves",
        "finished-enh:edu.unc.lib.dl.cdr.services.imaging.ImageEnhancementService",
        "failed-enh:edu.unc.lib.dl.cdr.services.imaging.ImageEnhancementService",
        "finished-enh:edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService",
        "failed-enh:edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService",
        "finished-enh:edu.unc.lib.dl.cdr.services.solr.SolrUpdateEnhancementService",
        "failed-enh:edu.unc.lib.dl.cdr.services.solr.SolrUpdateEnhancementService",
        "finished-enh:edu.unc.lib.dl.cdr.services.text.FullTextEnhancementService",
        "failed-enh:edu.unc.lib.dl.cdr.services.text.FullTextEnhancementService",
        "finished-enh:edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancementService",
        "failed-enh:edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancementService"
    };

    private CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(NEW_LINE_SEPARATOR);

    @Autowired
    private String dataPath;

    @Autowired
    private JedisPool jedisPool;

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public Set<String> getDepositMetrics() {
        try (Jedis jedis = getJedisPool().getResource()) {
            return jedis.keys("deposit-metrics:*");
        }
    }

    private Boolean buildFile(String path) {
        File csvFile = new File(path);
        if (csvFile.exists() && !csvFile.isDirectory()) {
            Long currentTime = System.currentTimeMillis();
            Long fileCreationTime = csvFile.lastModified();

            if ((currentTime - fileCreationTime ) > MILLISECONDS_IN_ONE_HOUR) {
                return true;
            }
            return false;
        } else if (!csvFile.exists()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets totals for metrics that are only aggregated daily such as moves & enhancements
     * It also grabs throughput totals for older data, before these fields were set to measure
     * performance at the individual deposit level
     * @return
     */
    public String getOperationsData() {
        String filePath = dataPath + "ingest-times-daily.csv";

        if (buildFile(filePath)) {
            Set<String> deposits = getDepositMetrics();
            try (Jedis jedis = getJedisPool().getResource()) {
                FileWriter fileWriter = null;
                Set<String> operations = null;
                Map<String, String> depositJob = null;
                Map<String, String> operationJob = null;
                String[] depositKeys = null;

                operations = jedis.keys("operation-metrics:*");

                fileWriter = new FileWriter(filePath);

                try (CSVPrinter csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat)) {
                    csvFilePrinter.printRecord(FILE_HEADERS);

                    Boolean matchingDate = false;

                    for (String deposit : deposits) {
                        depositKeys = deposit.split(":");

                        // Ignore data for individual deposits by uuid. Only need the daily ones in this instance
                        if (depositKeys.length > 2) {
                            continue;
                        }

                        depositJob = jedis.hgetAll(deposit);

                        String jobDate = depositKeys[1];
                        String throughputFiles = depositJob.get("throughput-files");
                        String throughputBytes = depositJob.get("throughput-bytes");
                        String finished = depositJob.get("finished");
                        String failed = depositJob.get("failed");
                        String failedDepositJob = depositJob.get(
                                "failed-job:edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService");

                        for (String operation : operations) {
                            String operationDate = operation.split(":")[1];

                            if (operationDate.equals(jobDate)) {
                                operationJob = jedis.hgetAll(operation);

                                List<String> data = new ArrayList<>();

                                data.add(jobDate);
                                data.add("N/A");
                                data.add(throughputFiles);
                                data.add(throughputBytes);
                                data.add("0");
                                data.add("0");
                                data.add(finished);

                                for (String field : MOVES_ENHANCEMENTS_JOBS_ARRAY) {
                                    String fieldValue = operationJob.get(field);
                                    data.add(fieldValue);
                                }

                                data.add(failed);
                                data.add(failedDepositJob);

                                csvFilePrinter.printRecord(data);

                                matchingDate = true;
                                break;
                            } else {
                                matchingDate = false;
                            }
                        }

                        if (!matchingDate) {
                            List<String> data = new ArrayList<>();
                            data.add(jobDate);
                            data.add("N/A");
                            data.add(throughputFiles);
                            data.add(throughputBytes);
                            data.add("0");
                            data.add("0");
                            data.add(finished);

                            this.addEmptyFields(data, MOVES_ENHANCEMENTS_JOBS_ARRAY);

                            data.add(failed);
                            data.add(failedDepositJob);

                            csvFilePrinter.printRecord(data);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to write data to {}", filePath, e);
            }
        }

        try {
            return FileUtils.readFileToString(new File(filePath));
        } catch (IOException e) {
            log.error("Error unable to read file to string from filepath {}", filePath, e);
            return null;
        }
    }

    /**
     * Gets totals for metrics that happen on every deposit grouped by uuid
     * @return
     */
    public String getDepositsData() {
        String filePath = dataPath + "ingest-times-daily-deposit.csv";

        if (buildFile(filePath)) {
            try (Jedis jedis = getJedisPool().getResource()) {
                Set<String> deposits = getDepositMetrics();

                FileWriter fileWriter = null;
                CSVPrinter csvFilePrinter = null;

                fileWriter = new FileWriter(filePath);
                csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
                csvFilePrinter.printRecord(FILE_HEADERS);

                for (String deposit : deposits) {
                    String[] depositKeys = deposit.split(":");

                    // Ignore data for daily deposits. Only need the ones  by uuid in this instance
                    if (depositKeys.length < 3) {
                        continue;
                    }

                    Map<String, String> depositJob = jedis.hgetAll(deposit);

                    String jobDate = depositKeys[1];
                    String jobUUID = depositKeys[2];
                    String throughputFiles = depositJob.get("throughput-files");
                    String throughputBytes = depositJob.get("throughput-bytes");
                    String queuedDuration = depositJob.get("queued-duration");
                    String ingestDuration = depositJob.get("duration");

                    List<String> data = new ArrayList<String>();
                    data.add(jobDate);
                    data.add(jobUUID);
                    data.add(throughputFiles);
                    data.add(throughputBytes);
                    data.add(queuedDuration);
                    data.add(ingestDuration);
                    data.add("0");

                    this.addEmptyFields(data, MOVES_ENHANCEMENTS_JOBS_ARRAY);

                    data.add("0");
                    data.add("0");

                    csvFilePrinter.printRecord(data);
                }

                csvFilePrinter.close();
            } catch (Exception e) {
                log.error("Failed to write data to {}", filePath, e);
            }
        }

        try {
            return FileUtils.readFileToString(new File(filePath));
        } catch (IOException e) {
            log.error("Error unable to read file to string from filepath {}", filePath, e);
            return null;
        }
    }

    /**
     * Add default value for fields that don't return anything
     * @param data
     * @param arrayValues
     * @return
     */
    private List<String> addEmptyFields(List<String> data, String[] arrayValues) {
        int i = 0;
        while (i < arrayValues.length) {
            data.add("0");
            i++;
        }

        return data;
    }

    @RequestMapping(value = "performanceMonitor", method = RequestMethod.GET)
    public String performanceMonitor() {
        return "report/performanceMonitor";
    }

    @RequestMapping(value = "sendOperationsData", method = RequestMethod.GET)
    public @ResponseBody
    String sendOperationsData(HttpServletResponse response) {
        response.setContentType("text/plain; charset=utf-8");
        return getOperationsData();
    }

    @RequestMapping(value = "sendDepositsData", method = RequestMethod.GET)
    public @ResponseBody
    String sendDepositData(HttpServletResponse response) {
        response.setContentType("text/plain; charset=utf-8");
        return getDepositsData();
    }
}