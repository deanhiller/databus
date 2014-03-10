#!/bin/bash

cp conf/application.conf.demo conf/application.conf

play-1.3.x/play run -Xmx1024M 
#-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044
