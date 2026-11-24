package edu.unc.lib.boxc.tools.amq;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.apache.activemq.ActiveMQConnectionFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.ScopeType;

/**
 * Entry point for AMQ inspection and requeue commands.
 *
 * @author bbpennel
 */
@Command(name = "amq-utils", mixinStandardHelpOptions = true,
        description = "Inspect and requeue ActiveMQ messages.",
        subcommands = { CountCommand.class, InspectCommand.class, RequeueCommand.class, LongleafSubmitCommand.class })
public class AmqUtilsCli implements Runnable {
    static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";

    @Option(names = { "-c", "--config" },
            scope = ScopeType.INHERIT,
            description = "External properties file. Defaults to $AMQ_UTILS_CONFIG when set.")
    private Path configPath;

    @Option(names = "--broker-url",
            scope = ScopeType.INHERIT,
            description = "Broker URL. Overrides amq.brokerUrl in the external configuration.")
    private String brokerUrl;

    @Option(names = "--username",
            scope = ScopeType.INHERIT,
            description = "Optional broker username. Overrides amq.username in the external configuration.")
    private String username;

    @Option(names = "--password",
            scope = ScopeType.INHERIT,
            description = "Optional broker password. Prefer external configuration or AMQ_PASSWORD.")
    private String password;

    @Spec
    private CommandSpec commandSpec;

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    ActiveMQConnectionFactory createConnectionFactory() throws IOException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(configurationValue("amq.brokerUrl", brokerUrl,
                "AMQ_BROKER_URL", DEFAULT_BROKER_URL));
        factory.setUserName(configurationValue("amq.username", username, "AMQ_USERNAME", null));
        factory.setPassword(configurationValue("amq.password", password, "AMQ_PASSWORD", null));
        // Longleaf batches are serialized ArrayList<String> payloads.
        factory.setTrustedPackages(List.of("java.util"));
        return factory;
    }

    PrintWriter out() {
        return commandSpec.commandLine().getOut();
    }

    String configurationValue(String property, String commandValue, String environmentVariable,
            String defaultValue) throws IOException {
        if (commandValue != null) {
            return commandValue;
        }
        String configurationValue = loadConfiguration().getProperty(property);
        if (configurationValue != null) {
            return configurationValue;
        }
        String environmentValue = System.getenv(environmentVariable);
        return environmentValue == null || environmentValue.isBlank() ? defaultValue : environmentValue;
    }

    private Properties loadConfiguration() throws IOException {
        Path path = configPath;
        if (path == null) {
            String configuredPath = System.getenv("AMQ_UTILS_CONFIG");
            if (configuredPath == null || configuredPath.isBlank()) {
                return new Properties();
            }
            path = Path.of(configuredPath);
        }

        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(path)) {
            properties.load(stream);
        }
        return properties;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AmqUtilsCli()).execute(args);
        System.exit(exitCode);
    }
}
