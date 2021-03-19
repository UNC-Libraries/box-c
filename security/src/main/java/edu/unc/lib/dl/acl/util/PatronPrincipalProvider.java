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
package edu.unc.lib.dl.acl.util;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.IP_PRINC_NAMESPACE;
import static edu.unc.lib.dl.acl.util.RemoteUserUtil.getRemoteUser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service which provides patron principals for a request
 *
 * @author bbpennel
 */
public class PatronPrincipalProvider {
    public static final String FORWARDED_FOR_HEADER = "X-FORWARDED-FOR";

    private String patronGroupConfigPath;
    private List<IPAddressPatronPrincipalConfig> patronPrincConfigs;

    /**
     * Get patron principals for the user making the provided request
     * @param request
     * @return list of patron principals
     */
    public List<String> getPrincipals(HttpServletRequest request) {
        List<String> princs = new ArrayList<>();
        princs.add(AccessPrincipalConstants.PUBLIC_PRINC);
        String username = getRemoteUser(request);
        if (!StringUtils.isBlank(username)) {
            princs.add(AUTHENTICATED_PRINC);
        }

        String remoteAddr = request.getHeader(FORWARDED_FOR_HEADER);
        if (StringUtils.isBlank(remoteAddr)) {
            remoteAddr = request.getRemoteAddr();
        }

        BigInteger ipInteger = IPAddressPatronPrincipalConfig.ipToBigInteger(remoteAddr);
        patronPrincConfigs.stream()
                .filter(config -> config.inRange(ipInteger))
                .map(IPAddressPatronPrincipalConfig::getId)
                .forEach(princs::add);

        return princs;
    }

    /**
     * List the configured custom patron principals
     * @return
     */
    public List<IPAddressPatronPrincipalConfig> getConfiguredPatronPrincipals() {
        return patronPrincConfigs;
    }

    public void setPatronGroupConfigPath(String patronGroupConfigPath) {
        this.patronGroupConfigPath = patronGroupConfigPath;
    }

    /**
     * Initialize the provider by parsing configuration
     * @throws IOException
     */
    public void init() throws IOException {
        InputStream configStream = new FileInputStream(new File(patronGroupConfigPath));
        ObjectMapper mapper = new ObjectMapper();
        patronPrincConfigs = mapper.readValue(configStream,
                new TypeReference<List<IPAddressPatronPrincipalConfig>>() {});
        patronPrincConfigs.stream().forEach(config -> {
            if (StringUtils.isBlank(config.getName())) {
                throw new IllegalArgumentException("Field 'name' is required for patron groups");
            }
            if (StringUtils.isBlank(config.getId())) {
                throw new IllegalArgumentException("Field 'id' is required for patron groups");
            } else if (!config.getId().startsWith(IP_PRINC_NAMESPACE)){
                throw new IllegalArgumentException("Field 'id' for IP Principal configuration must begin with "
                        + IP_PRINC_NAMESPACE);
            }
            if (StringUtils.isBlank(config.getIpInclude())) {
                throw new IllegalArgumentException("Field 'ipInclude' is required for patron groups");
            }
        });
    }
}
