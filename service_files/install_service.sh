#!/bin/sh
# Install files and directories for using DataBus as a system service
#  run as sudo
#
# M. Hopcroft, hopcroft@hp.com
#

echo " "
echo "This script will create directories and copy files for using DataBus as a system service."

# Location of the DataBus web application files (play)
dbapproot=/opt/databus/webapp

user=`whoami`
origuser=`logname`

if [ $user != "root" ]; then
  echo " "
  echo "  ERROR: this script is designed to be run by root (sudo)"
  echo " "
  sleep 4
  exit 2
fi

cd $dbapproot
cd ../service_files
echo `pwd`

# Make directories
echo "  make directories..."
#  pid file and log file
for newdir in /var/run/databus /var/log/databus
do
	if [ ! -d $newdir ]; then
		mkdir -vp $newdir
		chmod a+rw $newdir
	fi
done

# Copy files
echo "  copy files..."
#  logfile configuration
cp -vfp service.logback.xml $dbapproot/conf
#  service script
cp -vfp databus /etc/init.d/databus
chmod a+x /etc/init.d/databus
#  service executable
cp -vfp startService.sh $dbapproot/startService.sh
chmod a+x $dbapproot/startService.sh

echo " done."


