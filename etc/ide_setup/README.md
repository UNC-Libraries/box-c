# Intellij Setup

## Configuration and Troubleshooting
You will need to set Intellij to use your Java 8 SDK installation:
https://www.jetbrains.com/help/idea/sdk.html#change-project-sdk

If Intellij is giving project errors related to not being able to find maven dependencies, the following steps may help:
* Right click on the pom.xml file for the project and go to Maven > Reload Project
* Perform a `mvn clean install -DskipTests` in the box-c project, then in intellij go to File > Invalidate Caches, and restart intellij.

## Code Style
Preferences > Editor > Code Style > Java

Click the gear menu next to the "Scheme" input box, and select "Import Scheme" > "Intellij IDEA code style XML".

Then navigate to and select `box-c/etc/ide_setup/boxc_code_profile.xml`

## Code Templates
Preferences > Editor > File and Code Templates

Under the "Files" tab, we want to update the template for the following types:
* Class
* Interface
* Enum
* AnnotationType

Overwrite the lines in the window on the right before the `public class` (etc) section with the following snippet:

```
#parse("File Header.java")

#if (${PACKAGE_NAME} && ${PACKAGE_NAME} != "")package ${PACKAGE_NAME};#end


/**
 * @author ${USER}
 */
```
And uncheck "Reformat according to style"

And then go with the "Includes" tab, select "File Header", and paste the following:
```
/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 ```
 This is based off the license header configured here:
 https://github.com/UNC-Libraries/box-c/blob/main/common-utils/src/main/resources/license/header.txt


# Eclipse setup

A version of Eclipse with m2e is required

In order to run tests in Eclipse, you will need to set an environment variable. Go to Preferences > Java > Installed JREs. Select your JRE and click Edit, then type  ```-Dfcrepo.baseUri=http://example.com/rest```` in the Default VM Arguments box in the Default VM Arguments box
