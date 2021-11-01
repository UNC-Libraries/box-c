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

IDE Developer Setup
----------------------------
See the instructions here:
[Setup Readme](etc/ide_setup/)


Running Tests
-------------

All tests run automatically in Github Actions.
All Java tests run automatically when building the project, unless skipped.
JavaScript test don't run on a maven build, but can be run manually using the NPM command below.

```
# Java Unit Tests (skipping tests from external modules)
mvn -pl '!clamav-java' test 
# Java unit and integration tests
mvn -pl '!clamav-java' verify 

# JavaScript Tests
npm --prefix static/js/vue-cdr-access run test:unit
```
