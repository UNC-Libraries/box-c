# Integration Tests

## Running individual tests in Intellij
For integration tests which depend on maven to set up additional applications (such as solr or the dcr access
application), the test needs to be run as a maven command rather than a regular junit test. To do this in Intellij,
go to:

* Run > Edit Configurations
* Click the "+" button in the top left and select "Maven" from the drop down.
* In the commandline field, enter a command like: `-Dit.test=CollectionsEndpointIT verify` where CollectionsEndpointIT is the name of the test you want to run.
* Set the "Working Directory" to the path to the integration module within your boxc project.
* Give a name to the test, such as "CollectionsEndpointIT verify", then click "Okay"
* The test can then be executed using "Run > Run...", or the test running dropdown menu in the top menu bar.

## Running the tests in the terminal
You will first need to build the project, which can be done using the following:
`mvn clean package -DskipTests`

Then to run all of the tests in the integration module, use:
`mvn verify -pl integration`
Or to run an individual test, use:
`mvn -Dit.test=CollectionsEndpointIT verify -pl integration`

## Creating New Web App Tests
### How to set up 
Choose which endpoint you'd like to test and create the test file in the correct web package
folder. The web app tests extend the ____ test class (TODO make parent test class), which has access to 
methods and contexts needed for setup. 

In the future the parent test class might be used to start the web server and set the system properties.

## Object Factories
When running integration tests, factory objects are useful for test data. They are located
in the factories package, and if one doesn't exist that you need for your test you may create one.

### Setting up a new factory
The parent factory is the `ContentObjectFactory` which holds validation and preparation
methods for adding created factory objects in Fedora and indexes them in the triple store
and Solr. New factories extend this parent factory in order to use these methods.

Each factory must specify a parent according to the type of object. For example, the 
AdminUnitFactory's parent is `ContentRootObject`. In the `AdminUnitFactory` creation method we find the 
reference to the `ContentRootObject` through the `repositoryObjectLoader` and add the 
`AdminUnitFactory` to it. That way solr will be able to find its parent object.

### Adding properties to and instantiating a factory
When creating a factory in a test, you'll need to pass in the options you'd like to specify 
for that factory. For example, to add a title and description to an AdminUnit factory:

```
var options = Map.of("title", "title1",
                     "abstract", "description1");
var adminUnit = adminUnitFactory.createAdminUnit(options);
```

#### Available properties
Currently, the only available options are:
- Title: using the key `title`
- Description: using the key `abstract`
