# AMQ Utilities CLI

`amq-utils-cli` is a standalone command-line tool for inspecting and requeuing ActiveMQ queues. It does not use Box-C application configuration or require a deployed web application.

Build the shaded executable from the repository root:

```bash
mvn -pl tools/amq-utils-cli package
```

Run it with:

```bash
java -jar tools/amq-utils-cli/target/amq-utils-cli-6.0-SNAPSHOT.jar --help
```

## Configuration

Connection settings are optional. With no settings, the tool connects to `tcp://localhost:61616`, which works well with an SSH tunnel. AMQ credentials are not required unless the broker requires them.

Supply settings as command-line options, environment variables, or an external properties file. Earlier sources take precedence.

```properties
amq.brokerUrl=tcp://localhost:61616
amq.username=example-user
amq.password=replace-with-deployed-secret
```

Use `--config /path/to/boxc/amq-utils-cli.properties` or set `AMQ_UTILS_CONFIG` to the external file path. Environment variables are `AMQ_BROKER_URL`, `AMQ_USERNAME`, and `AMQ_PASSWORD`.

## Commands

All queue names are JMS queue names, such as `ActiveMQ.DLQ` or `longleaf.dlq`.
`inspect` decodes Longleaf's `ObjectMessage` payloads, which are serialized
`ArrayList<String>` batches, as well as text, bytes, and map messages.

```bash
# Count messages without changing the queue.
java -jar amq-utils-cli.jar count --queue ActiveMQ.DLQ

# Inspect up to 20 messages, including properties and body previews.
java -jar amq-utils-cli.jar inspect --queue ActiveMQ.DLQ --limit 20

# Select messages with a standard JMS selector.
java -jar amq-utils-cli.jar inspect --queue longleaf.dlq \
  --selector "CamelFailureRouteId LIKE '%/register.longleaf%'"

# Select messages created after a UTC timestamp. This is combined with --selector when both are used.
java -jar amq-utils-cli.jar inspect --queue longleaf.dlq \
  --after 2026-11-01T00:00:05

# Requeue is a dry run until --execute is explicitly supplied.
java -jar amq-utils-cli.jar requeue --queue longleaf.dlq \
  --destination longleaf.register.batch \
  --selector "CamelFailureRouteId LIKE '%/register.longleaf%'" \
  --limit 50

java -jar amq-utils-cli.jar requeue --queue longleaf.dlq \
  --destination longleaf.register.batch \
  --selector "CamelFailureRouteId LIKE '%/register.longleaf%'" \
  --limit 50 --execute
```

The requeue command first browses the selected message IDs, then moves each one in its own JMS transaction. Messages that disappear between browse and consume are reported by the final moved count and are not removed from the source queue.

`--after` interprets its timezone-less value as UTC and selects messages with a `JMSTimestamp` strictly greater than that time.
