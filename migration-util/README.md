# Box-c version 3 to 5 Migration Utility
#### To build:
`mvn clean install`
or
`mvn package -pl migration-util`

#### Usage:
`java -jar migration-util/target/dcr-migration-util.jar`

#### To debug the built jar:
`java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y -jar migration-util/target/dcr-migration-util.jar`