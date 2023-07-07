# Intellij Setup

## Configuration and Troubleshooting
You will need to set Intellij to use your Java 11 SDK installation:
https://www.jetbrains.com/help/idea/sdk.html#change-project-sdk

If Intellij is giving project errors related to not being able to find maven dependencies, the following steps may help:
* Right click on the pom.xml file for the project and go to Maven > Reload Project
* Perform a `mvn clean install -DskipTests` in the box-c project, then in intellij go to File > Invalidate Caches, and restart intellij.

## Code Style
Preferences > Editor > Code Style > Java

Click the gear menu next to the "Scheme" input box, and select "Import Scheme" > "Intellij IDEA code style XML".

Then navigate to and select `box-c/etc/ide_setup/boxc_code_profile.xml`

# Eclipse setup

A version of Eclipse with m2e is required

In order to run tests in Eclipse, you will need to set an environment variable. Go to Preferences > Java > Installed JREs. Select your JRE and click Edit, then type  ```-Dfcrepo.baseUri=http://example.com/rest```` in the Default VM Arguments box in the Default VM Arguments box
