Building the project
---------------------
```
# clone the project
git clone https://github.com/UNC-Libraries/Carolina-Digital-Repository
# initialize submodules
git submodule update --init --recursive
# Build the project
mvn clean install -DskipTests
```

Eclipse IDE Developer Setup
---------------------------
A version of Eclipse with m2e is required

To set the environment variable you'll need for running unit tests in Eclipse, go to Preferences > Java > Installed JREs. Select your JRE and click Edit, then type  ```-Dfcrepo.baseUri=http://example.com/rest```` in the Default VM Arguments box in the Default VM Arguments box
