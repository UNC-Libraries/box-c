# Integration Tests

## Creating New Tests
### How to set up 
Choose which endpoint you'd like to test and create the test file in the correct web package
folder. The test will extend the ____ test class, which has access to methods and contexts
that start the web server and set the system properties.

## Object Factories
When running integration tests, factory objects are useful for test data. They are located
in the factories package.

### Setting up a new factory
The parent factory is the `ContentObjectFactory` which holds validation and preparation
methods for adding created factory objects in Fedora and indexes them in the triple store
and Solr. New factories extend this parent factory in order to use these methods.

### Adding properties to and instantiating a factory

#### Available properties

