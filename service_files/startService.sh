#!/bin/bash
# This script starts DataBus for production use as a background process.
#  DataBus is developed using the play framework.
#  The script starts the webapp using play in background mode.
#  You must create the directories for the pid file (/var/run/databus/) and the
#   log files (/var/log/databus/) before using this script.
#
# This script is compatible with the init.d-style system service script
#   named "databus"
#
# M. Hopcroft, hopcroft@hp.com
# Sep2013

# Location of the DataBus web application files (play)
dbapproot=/opt/databus/webapp
# pid file for the system service
#  Note: you must create this directory
#  Note: this must be the same as the directory in the databus service script
PIDFILE=/var/run/databus/databus.pid

# The logback file defines the logging parameters
#  If using "service" logback file, you must create /var/log/databus/
if [ -e $dbapproot/conf/service.logback.xml ]; then
	cp $dbapproot/conf/service.logback.xml $dbapproot/conf/logback.xml
fi

# Start the webapp
$dbapproot/play-1.2.5/play start $dbapproot --%prod --silent -Xmx1024M --pid_file=$PIDFILE
exit 0

# Debug options
#-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044
