![Build](https://github.com/UNC-Libraries/box-c/workflows/DcrBuild/badge.svg)

Requirements
============
Box-c requires Java 11 in order to build and test. On a Mac, this can be installed using brew as follows:
```
brew tap adoptopenjdk/openjdk
brew install --cask adoptopenjdk11
```
Then set `JAVA_HOME` to the installed version. This can be done by editing your .bash_profile or .bashrc file to add the following line:
```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home/
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
# Build development vue access app
npm --prefix static/js/vue-cdr-access run build-dev
# Build production vue access app
npm --prefix static/js/vue-cdr-access run build
# Build development vue permissions app
npm --prefix static/js/admin/vue-permissions-editor run build-dev
# Build production vue permissions app
npm --prefix static/js/admin/vue-permissions-editor run build
# Install new NPM package
npm install <package-name> --save
# Install a new NPM package used only in development, for example Jest, that's only used to run test
npm install <package-name> --saveDev
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

### Running java unit tests
```
# Java Unit Tests (skipping tests from external modules)
mvn -pl '!clamav-java' test 
```

### Running java unit and integration tests
To run the integration tests locally, you will first need to start external dependencies such as Fedora. Do to this, from the root directory of this project, run the following command:
```
docker-compose up
```
And then wait for the start-up/installation process to complete, which may take a few minutes.

Note: After the first time, you can start and stop the docker containers directly in Docker Desktop instead of using the commandline. It would still be best to wait a minute or two for all the containers to finish starting.

Next, run the integration tests from within your IDE or via the following:
```
# Java unit and integration tests
mvn -pl '!clamav-java' verify 
```

### Running JavaScript tests
```
# JavaScript Tests
npm --prefix static/js/admin/vue-cdr-admin run test
npm --prefix static/js/vue-cdr-access run test
```

Creating a New Vue Project
--------------------------

run `npm init vue@3` and follow the prompts if you need to scaffold a new Vue project. This will create a vite based 
application similar to our current Vue applications.
