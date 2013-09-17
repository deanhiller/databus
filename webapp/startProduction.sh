#!/bin/bash

cp conf/prod.logback.xml conf/logback.xml

#NOTE: Developers, PLEASE do not start this script and start using the 
#production database!!!!
play-1.2.5/play run --%prod -Xmx1024M 
#-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044
