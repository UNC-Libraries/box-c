#!/bin/bash

export JAVA_HOME=`/usr/libexec/java_home -v 1.8`

NAME="Deposit Service"
DEPOSIT_HOME="target"
DEPOSIT_OUT=deposit-jsvc.out
DEPOSIT_ERR=error.err
STARTUP_WAIT=3000
JAVA_CLASSPATH="./deposit.jar"
JAVA_OPTS="-debug -Xdebug -Xrunjdwp:transport=dt_socket,address=48001,server=y,suspend=n -Ddistribution.dir=$DEPOSIT_HOME -Ddeposit.properties.uri=classpath:deposit.properties -Dacl.properties.uri=file:../../etc/acl-config.properties -Dlog4j.configuration=file:../src/test/resources/log4j.properties -Xmx256M"
JSVC_CLASS="edu.unc.lib.deposit.DepositDaemon"
JSVC_OPTS="-cp $JAVA_CLASSPATH -pidfile deposit.pid -cwd $DEPOSIT_HOME -outfile $DEPOSIT_OUT -errfile $DEPOSIT_ERR -wait $STARTUP_WAIT"

[ -f /etc/sysconfig/$NAME ] && . /etc/sysconfig/$NAME

start() {
  echo -n "Starting $NAME: "
  echo "jsvc $JSVC_OPTS $JAVA_OPTS $JSVC_CLASS"
  jsvc $JSVC_OPTS $JAVA_OPTS $JSVC_CLASS
  RETVAL=$?
  if [ $RETVAL -eq 0 ]; then
    echo $RETVAL
  else
    echo $RETVAL
  fi
  echo
  return $RETVAL
}

stop() {
  echo -n "Stopping $NAME: "
  PID=$(cat target/deposit.pid)
  kill $PID
  RETVAL=$?
  echo
  return $RETVAL
}

trap stop EXIT
start