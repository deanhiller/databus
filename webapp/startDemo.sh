#!/bin/bash

cp conf/prod.logback.xml conf/logback.xml
cp conf/application.conf.qa conf/application.conf

play-1.2.5/play run -Xmx1024M 
#-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044
