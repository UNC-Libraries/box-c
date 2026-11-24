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

## Longleaf Submission

Submit registration requests from a file containing one Fedora URI or object identifier per line. Bare UUIDs resolve beneath `content`; use `deposits/<uuid>` for a deposit record. An optional component path is supported, such as `deposits/<uuid>/md/event_log`. The command walks `ldp:contains` relations and submits only Longleaf-registerable binary resources.

`fcrepo.requestBaseUrl` (or `--fcrepo-request-base-url`) is the Fedora HTTP endpoint, so it can point to a local tunnel. `fcrepo.baseUri` (or `--fcrepo-base-uri`) is the canonical repository URI sent to Longleaf; it defaults to `fcrepo.requestBaseUrl`. This allows, for example, requests through `http://localhost:8181/fcrepo/rest` while Longleaf receives URIs rooted at the repository hostname. Fedora settings may also be supplied with `FCREPO_REQUEST_BASE_URL`, `FCREPO_BASE_URI`, `FCREPO_USERNAME`, and `FCREPO_PASSWORD` environment variables.

```bash
# Preview eligible binaries without creating messages.
java -jar amq-utils-cli.jar longleaf-submit --action register --input updated-objects.txt

# Submit eligible binaries to longleaf.register.batch.
java -jar amq-utils-cli.jar longleaf-submit --action register --input updated-objects.txt --execute

# Deregistration input is one file: URI per line.
java -jar amq-utils-cli.jar longleaf-submit --action deregister --input removed-files.txt --execute
```
