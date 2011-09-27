
CONTENTS

1. INSTALL
1.1 REQUIREMENTS
1.2 DOWNLOAD AND EXTRACT
1.3 EDITING THE CONFIGURATION FILE
1.4 EDIT THE BUILD FILE
1.5 RUN THE BUILD SCRIPT
1.6 VIEW RESULTS
2. MAINTENANCE
2.1 ADDING USERS
3. EXTENDING THE FEDORA CODE BASE
3.1 CHANGING THE WAY SERVICE DOCUMENTS ARE GENERATED
3.2 ADDING NEW FILE HANDLERS
3.3 CLASS HIERARCHY

1. Install
----------
	
1.1 Requirements
----------------

This software requires the following packages:

Fedora 2.2 or higher
tomcat
java 1.5+
ant

1.2 DOWNLOAD AND EXTRACT
------------------------

Download
Extract 
tar zxvf sword-fedora.tar.gz

1.3 Editing the configuration file
----------------------------------

Edit sword-fedora/conf/properties.xml

change the following:

1. external_obj_url to http://FEDORA_HOST:FEDORA_PORT/fedora/get/##PID##

where
 * FEDORA_HOST is the public hostname of fedora
 * FEDORA_PORT is the port number fedora is running on

2. external_ds_url to http://FEDORA_HOST:FEDORA_PORT/fedora/get/##PID##

where
 * FEDORA_HOST is the public hostname of fedora
 * FEDORA_PORT is the port number fedora is running on

3. host to the fedora host (can be localhost or the public hostname)

4. port change the port to the fedora port

5. pid_namespace change to your pid namespace default can be found in $FEDORA_HOME/server/config/fedora.fcfg property pidNamespace

6. temp_dir change to where you would like the temp directory to be

7. reposiotry_uri change to a unique identifier for your repository

8. Edit the service document to your requirements see config file for comments

1.4 Edit the build file
-----------------------

edit build.xml

change the following:

<property name="tomcat"  location="/usr/local/jakarta-tomcat-5.0.28"/>

to the location of your tomcat instillation

1.5 Run the build script
------------------------

ant dist

1.6 View Results
-----------------

Point the client to http://TOMCAT_HOST:TOMCAT_PORT/sword/app/servicedocument to retrieve a service document (this link also work in a web browser). If you haven't added any new users you can use the fedoraAdmin username and password.

Where
 * TOMCAT_HOST is the public host name for the tomcat server where the sword code is installed. This can be the Fedora installed tomcat
 * TOMCAT_PORT is the port number tomcat runs off

There are some example deposits in the doc directory to try out.

2. Maintenance
2.1 Adding Users

Edit $FEDORA_HOME/server/config/fedora-users.xml and add the following:

<user name="sword" password="sword">
   <attribute name="fedoraRole">
        <value>administrator</value>
   </attribute>
</user>

3. EXTENDING THE FEDORA CODE BASE
3.1 CHANGING THE WAY SERVICE DOCUMENTS ARE GENERATED

Currently the service document is generated from the config file but this could be replaced by a more dynamically generated service document. To do this create a new class which extends org.purl.sword.server.fedora.FedoraServer and override the method:

protected ServiceDocument getServiceDocument(final String pOnBehalfOf) throws SWORDException {

To get the new sub class to be used instead of FedoraServer edit the web.conf and change the following:

<context-param>
    <param-name>server-class</param-name>
    <param-value>org.purl.sword.server.fedora.FedoraServer</param-value>
    <description>
      The SWORSServer class name
    </description>
</context-param>

so that param-value points to the new sub class.

3.2 ADDING NEW FILE HANDLERS

To add a new file handler create a class which implements org.purl.sword.server.fedora.fileHandlers.FileHandler and has a default constructor that takes no arguments and throws no exceptions. Then edit the configuration file and add your new class to the following section:

<file_handlers>
        <handler class="org.purl.sword.server.fedora.fileHandlers.JpegHandler" />
        <handler class="org.purl.sword.server.fedora.fileHandlers.METSFileHandler" />
        <handler class="org.purl.sword.server.fedora.fileHandlers.ZipFileHandler" />
	<handler class="org.purl.sword.server.fedora.fileHandlers.ZipMETSFileHandler" />
</file_handlers>

Ensure your new class is in the CLASSPATH when the application gets copied to tomcat.

To make things easier you can extend org.purl.sword.server.fedora.fileHandlers.DefaultFileHandler and only overload the methods which you are interested in. See the api for more details.

To create Fedora objects use the Classes in org.purl.sword.server.fedora.fedoraObjects and then call FedoraObject.ingest to get them into Fedora.

3.3 Class Hierarchy

The Hierarchy for the datastream package is a bit complicated so it is shown below:

					    DATASTREAM
						|
						|
						|
------------------------------------------------------------------------------------------------
|						|						|
| 						|						|
|						|						|
LocalDatastream				InlineDatastream                               ManagedDatastream
                                                |
                                                |
                                                | 						
			--------------------------------------------------
			|			|			|
			|			|			|
			|			|			|
		    DublinCore		    Relationship	     XMLInlineDatastream


The fileHandlers all extend DefaultFileHandler

