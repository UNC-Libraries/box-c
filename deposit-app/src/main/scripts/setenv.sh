#!/bin/bash

JSVC_EXECUTABLE="$( which jsvc )"
JSVC_PID_FILE=/var/run/deposit-daemon.pid

if [ -z "$JSVC_USER" ]; then
	JSVC_USER="$USER"
fi

DIST_DIR=/opt/deposit
LIB_DIR="$DIST_DIR/lib"
CONF_DIR="$DIST_DIR/conf"

JAVA_EXEC="$( which java )"
JAVA_CLASSPATH="$CONF_DIR:$LIB_DIR/commons-daemon-${commons-daemon.version}.jar:$LIB_DIR/${project.build.finalName}-${project.version}.${project.packaging}"
JAVA_MAIN_CLASS="edu.unc.lib.deposit.DepositDaemon"
JAVA_OPTS="-Ddistribution.dir=$DIST_DIR -Ddeposit.properties.uri=file:$CONF_DIR/deposit.properties"

# uncomment the following line to enable a JDWP debugger on port 8001 
JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,address=8001,server=y,suspend=n $JAVA_OPTS"

if [ -z "$JAVA_HOME" ]; then
	JAVA_HOME="$( $JAVA_EXEC -cp "$JAVA_CLASSPATH" -server \
		edu.unc.lib.deposit.GetProperty java.home )"
fi

export JSVC_EXECUTABLE JSVC_PID_FIL JSVC_USER DIST_DIR JAVA_OPTS CONF_DIR JAVA_EXEC \
	JAVA_CLASSPATH JAVA_MAIN_CLASS JAVA_HOME
