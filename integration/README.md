# Integration Tests

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
