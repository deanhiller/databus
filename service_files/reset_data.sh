#!/bin/sh
# Delete the cassandra data directories in order to reset a DataBus installation
# This will delete all data, user info, charts, etc from DataBus.
# run as sudo
#
# M. Hopcroft, hopcroft@hp.com
#

currentdir=`pwd`

user=`whoami`
if [ $user != "root" ]; then
  echo " "
  echo "  ERROR: this script is designed to be run by root (sudo)"
  echo " "
  sleep 4
  exit 2
fi

echo "WARNING: This will wipe out all of the data in your DataBus installation"
echo "Cancel now with Ctrl-C if you do not want to do this..."
sleep 4

echo " Stopping DataBus:"
service databus stop
echo " Stopping Cassandra:"
service cassandra stop
echo "Deleting Cassandra database data..."
cd /var/lib/cassandra
for cass_dir in data commitlog
do
	rm -vrf $cass_dir
	mkdir -v $cass_dir
	chown cassandra:cassandra $cass_dir
	chmod 700 $cass_dir
done
echo " done."
service cassandra start
cd $currentdir
echo "Start DataBus when ready (service databus start)."
#service databus start


