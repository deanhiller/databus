#!/bin/bash

cp conf/application.conf.qa conf/application.conf

#NOTE: Developers, PLEASE do not start this script and start using the 
#production database!!!!
play1.3.x/play run -Xmx1024M 
#-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044
