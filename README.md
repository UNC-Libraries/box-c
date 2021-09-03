![Build](https://github.com/UNC-Libraries/box-c/workflows/DcrBuild/badge.svg)

Building the project
---------------------
```
# clone the project
git clone https://github.com/UNC-Libraries/box-c
# initialize submodules
git submodule update --init --recursive
# Install SASS parser to build CSS
gem install sass
# Install Homebrew, if not already installed
See https://brew.sh/ for instructions.
# Install Node.js to build JavaScript and run JavaScript tests
brew install node
# Build the project
mvn clean install -DskipTests
```

Eclipse IDE Developer Setup
---------------------------
A version of Eclipse with m2e is required

To set the environment variable you'll need for running unit tests in Eclipse, go to Preferences > Java > Installed JREs. Select your JRE and click Edit, then type  ```-Dfcrepo.baseUri=http://example.com/rest```` in the Default VM Arguments box in the Default VM Arguments box

Running Tests
-------------

All tests run automatically in Travis.
All Java tests run automatically when building the project.
JavaScript test don't run on a maven build, but can be run manually using the NPM command below.

```
# Java Tests
mvn clean install

# JavaScript Tests
npm --prefix static/js/vue-cdr-access run test:unit
```
