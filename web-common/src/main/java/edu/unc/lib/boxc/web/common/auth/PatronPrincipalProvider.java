package edu.unc.lib.boxc.web.common.auth;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.IP_PRINC_NAMESPACE;
import static edu.unc.lib.boxc.web.common.auth.RemoteUserUtil.getRemoteUser;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.boxc.auth.api.AccessPrincipalConstants;

/**
 * Service which provides patron principals for a request
 *
 * @author bbpennel
 */
public class PatronPrincipalProvider {
    private static final Logger log = getLogger(PatronPrincipalProvider.class);

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
            return princs;
        }

        BigInteger ipInteger = IPAddressPatronPrincipalConfig.ipToBigInteger(remoteAddr);
        patronPrincConfigs.stream()
                .filter(config -> config.inRange(ipInteger))
                .map(IPAddressPatronPrincipalConfig::getPrincipal)
                .forEach(princs::add);

        log.debug("Returning patron principals {}", princs);

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
            if (StringUtils.isBlank(config.getPrincipal())) {
                throw new IllegalArgumentException("Field 'principal' is required for patron groups");
            } else if (!config.getPrincipal().startsWith(IP_PRINC_NAMESPACE)) {
                throw new IllegalArgumentException("Field 'principal' for IP Principal configuration must begin with "
                        + IP_PRINC_NAMESPACE);
            }
            if (StringUtils.isBlank(config.getIpInclude())) {
                throw new IllegalArgumentException("Field 'ipInclude' is required for patron groups");
            }
        });
    }
}
