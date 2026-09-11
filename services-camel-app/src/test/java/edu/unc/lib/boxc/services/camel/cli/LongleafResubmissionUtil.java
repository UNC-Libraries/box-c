package edu.unc.lib.boxc.services.camel.cli;

import jakarta.jms.Connection;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.activemq.ActiveMQConnectionFactory;

import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;

/**
 * Standalone CLI utility for resubmitting file paths to the longleaf register or deregister
 * batch queues, for cases where files need to be reprocessed by longleaf outside of the
 * normal event driven flow. Not shipped with the deployed application, intended to be run
 * manually against a local or remote ActiveMQ broker.
 *
 * For the "register" action, message bodies are the PID/datastream component identifier
 * (e.g. {uuid}/datafs/original_file), matching the identifier form expected by
 * RegisterToLongleafProcessor. For "deregister", message bodies are file:// URIs, matching
 * the form expected by DeregisterLongleafProcessor.
 *
 * Usage:
 *   LongleafResubmissionUtil --input &lt;path&gt; --action &lt;register|deregister&gt;
 *       [--brokerUrl &lt;url&gt;] [--username &lt;user&gt;] [--password &lt;pass&gt;]
 *
 * @author bbpennel
 */
public class LongleafResubmissionUtil {
    private static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";

    private static final String REGISTER_DESTINATION = "activemq:queue:longleaf.register.batch";
    private static final String DEREGISTER_DESTINATION = "activemq:queue:longleaf.deregister.batch";
    private static final String REGISTER_ACTION = "register";
    private static final String DEREGISTER_ACTION = "deregister";

    // Matches a UUID path segment, used to locate the start of the PID identifier
    // within a hashed storage path such as .../2e/4a/84/18/{uuid}/datafs/original_file
    private static final Pattern UUID_SEGMENT_PATTERN =
            Pattern.compile("^" + RepositoryPathConstants.UUID_PATTERN + "$");

    private LongleafResubmissionUtil() {
    }

    public static void main(String[] args) throws Exception {
        String inputPath = null;
        String action = null;
        String brokerUrl = DEFAULT_BROKER_URL;
        String username = DEFAULT_USERNAME;
        String password = DEFAULT_PASSWORD;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--input":
                    inputPath = args[++i];
                    break;
                case "--action":
                    action = args[++i];
                    break;
                case "--brokerUrl":
                    brokerUrl = args[++i];
                    break;
                case "--username":
                    username = args[++i];
                    break;
                case "--password":
                    password = args[++i];
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized argument: " + arg);
            }
        }

        if (inputPath == null) {
            throw new IllegalArgumentException("--input is required");
        }
        if (!REGISTER_ACTION.equals(action) && !DEREGISTER_ACTION.equals(action)) {
            throw new IllegalArgumentException("--action is required and must be either '"
                    + REGISTER_ACTION + "' or '" + DEREGISTER_ACTION + "'");
        }

        String destination = REGISTER_ACTION.equals(action) ? REGISTER_DESTINATION : DEREGISTER_DESTINATION;
        boolean register = REGISTER_ACTION.equals(action);

        List<String> paths = Files.readAllLines(Paths.get(inputPath));
        System.out.println("Preparing to send " + paths.size() + " files with action " + action + " to " + destination);

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(username, password, brokerUrl);
        try (Connection connection = factory.createConnection()) {
            connection.start();
            try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                Queue queue = session.createQueue(destination);
                try (MessageProducer producer = session.createProducer(queue)) {
                    int sent = 0;
                    for (String line : paths) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty()) {
                            continue;
                        }
                        String messageBody = register ? pathToPidForm(trimmed) : pathToFileUri(trimmed);
                        TextMessage message = session.createTextMessage(messageBody);
                        producer.send(message);
                        sent++;
                        if (sent % 50 == 0) {
                            System.out.println("Sent " + sent + " / " + paths.size() + " files");
                        }
                    }
                    System.out.println("Finished sending " + sent + " messages");
                }
            }
        }
    }

    private static String pathToFileUri(String filePath) {
        return Paths.get(filePath).toUri().toString();
    }

    /**
     * Extracts the PID/datastream component identifier from a hashed storage file path,
     * e.g. converts /mnt/.../2e/4a/84/18/{uuid}/datafs/original_file into
     * {uuid}/datafs/original_file.
     *
     * @param filePath storage file path containing a UUID path segment
     * @return the PID identifier form, starting from the UUID segment onward
     */
    private static String pathToPidForm(String filePath) {
        Path path = Paths.get(filePath);
        int uuidIndex = -1;
        for (int i = 0; i < path.getNameCount(); i++) {
            if (UUID_SEGMENT_PATTERN.matcher(path.getName(i).toString()).matches()) {
                uuidIndex = i;
                break;
            }
        }
        if (uuidIndex == -1) {
            throw new IllegalArgumentException("Unable to locate a UUID path segment in: " + filePath);
        }
        return path.subpath(uuidIndex, path.getNameCount()).toString();
    }
}
