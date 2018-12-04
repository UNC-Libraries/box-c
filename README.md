Building the project
--------------------
```
# clone the project
git clone https://github.com/UNC-Libraries/Carolina-Digital-Repository
# initialize submodules
git submodule update --init --recursive
# Install SASS parser to build CSS
gem install sass
# Build the project
mvn clean install -DskipTests
```

Eclipse IDE Developer Setup
---------------------------
A version of Eclipse with m2e is required
