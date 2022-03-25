![Build](https://github.com/UNC-Libraries/box-c/workflows/DcrBuild/badge.svg)

Requirements
============
Box-c requires Java 8 in order to build and test. On a Mac, this can be installed using brew as follows:
```
brew tap adoptopenjdk/openjdk
brew install --cask adoptopenjdk8
```
Then set `JAVA_HOME` to the installed version. This can be done by editing your .bash_profile or .bashrc file to add the following line:
```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/
```

Maven 3.x is required in order to build the project, which can be installed with:
```
brew install maven
```


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
npm --prefix static/js/admin/vue-permissions-editor run test
npm --prefix static/js/vue-cdr-access run test
```
