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
red='\e[0;31m'
NC='\e[0m' # No Color

jsvc_exec()
{  
    cd $DIST_DIR
    $JSVC_EXECUTABLE $STOP -server -cp "$JAVA_CLASSPATH" -wait 30 -user $JSVC_USER -outfile $LOG_OUT -errfile $LOG_ERR -pidfile $JSVC_PID_FILE $JAVA_OPTS $JAVA_MAIN_CLASS $ARGS
}

case "$1" in
    start) 
        echo "Starting the $DESC..."       
       
        # Start the service
        jsvc_exec
        RETVAL=$?
       
        [ $RETVAL -eq 0 ] && echo "The $DESC has started."
        [ $RETVAL -ne 0 ] && echo -e "${red}The $DESC failed to start.${NC}" >&2
        exit $RETVAL
    ;;
    stop)
        echo "Stopping the $DESC..."
        if [ -f "$JSVC_PID_FILE" ]; then
       
        	# Stop the service
        	STOP="-stop"
        	jsvc_exec
        	RETVAL=$?      
       
        	[ $RETVAL -eq 0 ] && echo "The $DESC has stopped."
        	[ $RETVAL -ne 0 ] && echo -e "${red}The $DESC failed to stop.${NC}" >&2
        	exit $RETVAL
        else
            echo "Daemon not running, no action taken"
            exit 1
        fi
    ;;
    restart)
        if [ -f "$JSVC_PID_FILE" ]; then
           
            echo "Restarting the $DESC..."
           
            # Stop the service
            jsvc_exec "-stop"
            
            # Start the service
            jsvc_exec
            RETVAL=$?
           
            [ $RETVAL -eq 0 ] && echo "The $DESC has restarted."
        	[ $RETVAL -ne 0 ] && echo -e "${red}The $DESC failed to restart.${NC}" >&2
            exit $RETVAL
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
