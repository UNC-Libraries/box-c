# Box-c version 3 to 5 Migration Utility
#### To build:
`mvn clean install`
or
`mvn package -pl migration-util`

#### Usage:
`java -jar migration-util/target/dcr-migration-util.jar`

#### To debug the built jar:
`java -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y -jar migration-util/target/dcr-migration-util.jar`

#### Bulk transforming PREMIS
Transforming deposit record PREMIS
`java -Dfcrepo.baseUri=http://dcr.lib.unc.edu/fcrepo/rest/ -jar migration-util/target/dcr-migration-util.jar transform_premis -d /path/to/premis_transform/deposit_list.txt /path/to/premis_transform/deposit_out/`

## Populating path index
First, you need to generate lists of all object and datastream files:
```
sudo tree -fFi  /path/to/home/fedora/objects/ | ack --nopager -v "/$" > /tmp/bxc_objects.txt &
sudo tree -fFi  /path/to/home/fedora/datastreams/ | ack --nopager -v "/$" > /tmp/bxc_datastreams.txt &
```
This will take a long time.

Note: for the following commands you can specify the destination where the index database should be stored by setting the `dcr.migration.index.url` system property.

Next, populate the path index (<10 minutes):
```
java -jar migration-util/target/dcr-migration-util.jar path_index populate /tmp/bxc_objects.txt /tmp/bxc_datastreams.txt
```

You can now query the index:
```
# Count the number of files indexed
java -jar migration-util/target/dcr-migration-util.jar path_index count

# Get all the paths for a uuid
java -jar migration-util/target/dcr-migration-util.jar path_index get_paths <uuid>
```