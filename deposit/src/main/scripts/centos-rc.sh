#!/bin/bash
#
# deposit:      Deposit Daemon
#
# chkconfig: 35 99 01
#
# description: Starts and stops the Deposit Daemon service
#
# processname: deposit
# pidfile: /var/run/deposit-daemon.pid
#

. /opt/deposit/bin/setenv.sh

NAME="deposit"
DESC="Deposit Worker Daemon"
LOG_OUT=$DIST_DIR/logs/deposit-jsvc.out
LOG_ERR=$DIST_DIR/logs/deposit-jsvc.err

jsvc_exec()
{  
    cd $DIST_DIR
    $JSVC_EXECUTABLE -server -cp "$JAVA_CLASSPATH" -user $JSVC_USER -outfile $LOG_OUT -errfile $LOG_ERR -pidfile $JSVC_PID_FILE $JAVA_OPTS $JAVA_MAIN_CLASS $ARGS
}

case "$1" in
    start) 
        echo "Starting the $DESC..."       
       
        # Start the service
        jsvc_exec
       
        echo "The $DESC has started."
    ;;
    stop)
        echo "Stopping the $DESC..."
       
        # Stop the service
        jsvc_exec "-stop"      
       
        echo "The $DESC has stopped."
    ;;
    restart)
        if [ -f "$JSVC_PID_FILE" ]; then
           
            echo "Restarting the $DESC..."
           
            # Stop the service
            jsvc_exec "-stop"
           
            # Start the service
            jsvc_exec
           
            echo "The $DESC has restarted."
        else
            echo "Daemon not running, no action taken"
            exit 1
        fi
    ;;
    *)
    echo "Usage: /etc/init.d/$NAME {start|stop|restart}" >&2
    exit 3
    ;;
esac
