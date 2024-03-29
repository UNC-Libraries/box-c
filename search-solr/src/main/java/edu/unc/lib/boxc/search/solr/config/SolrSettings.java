package edu.unc.lib.boxc.search.solr.config;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class which stores Solr index addressing and instantiation settings from a properties file.
 *
 * @author bbpennel
 */
public class SolrSettings extends AbstractSettings {
    private final Logger LOG = LoggerFactory.getLogger(SolrSettings.class);
    private String path;
    private String url;
    private String core;
    private int socketTimeout;
    private int connectionTimeout;
    private int defaultMaxConnectionsPerHost;
    private int maxConnections;
    private int maxRetries;
    // Mapping of field keys to internal solr field names
    private Map<String, String> fieldNames;
    // Reverse of fieldName, for translating from the internal solr field name to the general field identification key
    private Map<String, String> fieldNameToKey;
    private String[] requiredFields = new String[] {
            SearchFieldKey.ADMIN_GROUP.getSolrField(), SearchFieldKey.ID.getSolrField(),
            SearchFieldKey.RESOURCE_TYPE.getSolrField(), SearchFieldKey.ROLE_GROUP.getSolrField(),
            SearchFieldKey.ROLLUP_ID.getSolrField(), SearchFieldKey.TITLE.getSolrField() };

    public SolrSettings() {
        fieldNames = new HashMap<>();
    }

    /**
     * Initialize SolrSettings attributes from a properties input object
     *
     * @param properties
     *           solr settings properties object.
     */
    public void setProperties(Properties properties) {
        LOG.debug("Setting properties.");
        this.setPath(properties.getProperty("solr.path", ""));
        this.setCore(properties.getProperty("solr.core", ""));
        this.setSocketTimeout(Integer.parseInt(properties.getProperty("solr.socketTimeout", "1000")));
        this.setConnectionTimeout(Integer.parseInt(properties.getProperty("solr.connectionTimeout", "100")));
        this.setDefaultMaxConnectionsPerHost(Integer.parseInt(properties.getProperty(
                "solr.defaultMaxConnectionsPerHost", "100")));
        this.setMaxConnections(Integer.parseInt(properties.getProperty("solr.maxConnections", "100")));
        this.setMaxRetries(Integer.parseInt(properties.getProperty("solr.maxRetries", "1")));

        // Store the URL to the Solr index for non-embedded connections. Add the core if specified.
        if (this.path != null) {
            this.url = this.path;
            if (this.core != null && !this.core.equals("")) {
                if (this.url.lastIndexOf("/") != this.url.length() - 1) {
                    this.url += "/";
                }
                this.url += this.core;
            }
        }

        populateMapFromProperty("solr.field.", fieldNames, properties);
        fieldNameToKey = getInvertedHashMap(fieldNames);

        LOG.debug(this.toStringStatic());
    }

    /**
     * Gets a SolrClient object using the current settings.
     *
     * @return
     */
    public SolrClient getSolrClient() {
        // TODO use HttpClient with config properties

        SolrClient solr = new HttpSolrClient.Builder(getUrl())
                .build();

        return solr;
    }

    private static Pattern escapeReservedWords
        = Pattern.compile("\\b(?<!\\*)(AND|OR|NOT)\\b(?!\\*)");

    public static String sanitize(String value) {
        if (value == null) {
            return value;
        }
        return escapeReservedWords.matcher(escapeQueryChars(value)).replaceAll("'$1'");
    }

    public static String escapeQueryChars(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
                    || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
                    || c == '?' || c == '|' || c == '&' || c == ';' || c == '/' || Character.isWhitespace(c)) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static Pattern splitTermFragmentsRegex = Pattern.compile("(\"(([^\"]|\\\")*)\"|([^\" ,]+))");

    /**
     * Retrieves all the search term fragments contained in the selected field. Fragments are either single words
     * separated by non-alphanumeric characters, or phrases encapsulated by quotes.
     *
     * @param value
     * @return
     */
    public static List<String> getSearchTermFragments(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = splitTermFragmentsRegex.matcher(value);
        List<String> fragments = new ArrayList<>();
        while (matcher.find()) {
            if (matcher.groupCount() == 4) {
                boolean quoted = matcher.group(2) != null;
                String fragment = quoted ? matcher.group(2) : matcher.group(4);
                fragment = sanitize(fragment.replace("\\\"", "\""));
                if (quoted || fragment.indexOf('\\') > -1) {
                    fragment = '"' + fragment + '"';
                }
                fragments.add(fragment);
            }
        }
        return fragments;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCore() {
        return core;
    }

    public void setCore(String core) {
        this.core = core;
    }

    public String[] getRequiredFields() {
        return requiredFields;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getDefaultMaxConnectionsPerHost() {
        return defaultMaxConnectionsPerHost;
    }

    public void setDefaultMaxConnectionsPerHost(int defaultMaxConnectionsPerHost) {
        this.defaultMaxConnectionsPerHost = defaultMaxConnectionsPerHost;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String toStringStatic() {
        String output = " path: " + path;
        output += "\n url: " + url;
        output += "\n core: " + core;
        output += "\n socketTimeout: " + socketTimeout;
        output += "\n connectionTimeout: " + connectionTimeout;
        output += "\n defaultMaxConnectionsPerHost: " + defaultMaxConnectionsPerHost;
        output += "\n maxConnections: " + maxConnections;
        output += "\n fieldNames: " + fieldNames;
        return output;
    }

    /**
     * Returns the dynamic field identification key for the internal solr field name given
     *
     * @param name
     * @return
     */
    public String getDynamicFieldKey(String name) {
        return fieldNameToKey.get(name);
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}
