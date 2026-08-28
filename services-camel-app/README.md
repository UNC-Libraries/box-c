# services-camel-app

Camel routes and processors for background services (indexing, longleaf storage
registration, solr updates, etc). See `src/main/webapp/WEB-INF/*.xml` for route/bean wiring.

## Dev/Admin CLI Utilities

Some standalone utilities live under `src/test/java` because they are only meant to be run
manually by a developer/admin against a running environment, and should not be packaged into
the deployed WAR.

### LongleafResubmissionUtil

`edu.unc.lib.boxc.services.camel.cli.LongleafResubmissionUtil`

Reads a newline-separated file of storage file paths and sends one JMS message per path to
the longleaf register or deregister batch queue on ActiveMQ.

The script determines an appropriate message body based on action, as register and deregister
expect different message bodies. The input file should contain absolute file paths to the files
to register or deregister.

```
mvn -pl services-camel-app exec:java \
  -Dexec.mainClass="edu.unc.lib.boxc.services.camel.cli.LongleafResubmissionUtil" \
  -Dexec.classpathScope=test \
  -Dexec.args="--input /path/to/paths.txt --action register"
```

*Arguments*

| Argument | Required | Default | Description |
|---|---|---|---|
| `--input` | yes | - | Path to a newline-separated file of storage file paths |
| `--action` | yes | - | `register` or `deregister` |
| `--brokerUrl` | no | `tcp://localhost:61616` | ActiveMQ broker URL |
| `--username` | no | `admin` | ActiveMQ username |
| `--password` | no | `admin` | ActiveMQ password |
