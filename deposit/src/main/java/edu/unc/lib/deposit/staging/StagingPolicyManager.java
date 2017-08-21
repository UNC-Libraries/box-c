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
package edu.unc.lib.deposit.staging;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.deposit.staging.StagingPolicy.CleanupPolicy;
import edu.unc.lib.dl.util.URIUtil;

/**
 * Manager for staging policies, which assists in determining if file paths are
 * located within accepted staging locations according to the provided policies,
 * and provides access to those policies. Loaded from a policy configuration
 * json file
 *
 * @author bbpennel
 *
 */
public class StagingPolicyManager {

    private static final Logger log = LoggerFactory.getLogger(StagingPolicyManager.class);

    private String basePath;

    private String configPath;

    private List<StagingPolicy> policies;

    public StagingPolicyManager() {
    }

    public void init() {
        loadConfig();
    }

    private void loadConfig() throws StagingException {
        File configFile = new File(configPath);

        ObjectMapper mapper = new ObjectMapper();

        try {
            // Load the policy configuration from the provided json
            policies = mapper.readValue(configFile, mapper.getTypeFactory()
                    .constructCollectionType(List.class, StagingPolicy.class));
        } catch (IOException e) {
            throw new StagingException("Failed to load staging configuration from " + configPath, e);
        }

        // Verify and resolve any staging location paths
        for (StagingPolicy policy : policies) {
            String path = policy.getPath();

            // Ensure that the path of the policy is absolute
            policy.setPath(makeAbsolute(path));

            // Abort loading if one of the paths is invalid
            if (!Paths.get(policy.getPath()).toFile().exists()) {
                throw new StagingException("Unable to resolve staging location " + policy.getPath());
            }
        }
    }

    /**
     * Get the staging policy which applies to the given file uri, or throw a
     * staging exception if no policy is found
     *
     * @param fileUri
     * @return
     * @throws StagingException
     */
    public StagingPolicy getStagingPolicy(URI fileUri) throws StagingException {
        String path = makeAbsolute(fileUri.getPath());

        for (StagingPolicy policy : policies) {
            String policyPath = policy.getPath();
            if (path.startsWith(policyPath)) {
                return policy;
            }
        }

        throw new StagingException("No staging policy available for " + path);
    }

    /**
     * Get the cleanup policy which applies to the given file uri, or throw a
     * staging exception if no policy is found
     *
     * @param fileUri
     * @return
     * @throws StagingException
     */
    public CleanupPolicy getCleanupPolicy(URI fileUri) throws StagingException {
        return getStagingPolicy(fileUri).getCleanupPolicy();
    }

    /**
     * Returns true if the given file uri is contained by at least one of the
     * staging locations registered with this manager
     *
     * @param fileUri
     * @return
     */
    public boolean isValidStagingLocation(URI fileUri) {
        try {
            getStagingPolicy(fileUri);
            return true;
        } catch (StagingException e) {
            return false;
        }
    }

    private String makeAbsolute(String path) {
        if (!Paths.get(path).isAbsolute()) {
            log.debug("Resolving relative path {} to within {}", path, basePath);
            return URIUtil.join(basePath, path);
        }
        return path;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }
}
