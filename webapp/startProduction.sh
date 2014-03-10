#!/bin/bash

cp conf/application.conf.prod conf/application.conf

#NOTE: Developers, PLEASE do not start this script and start using the 
#production database!!!!
play1.3.x/play run --%prod -Xmx1024M 
#-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044
