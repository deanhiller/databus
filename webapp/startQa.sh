#!/bin/bash

#NOTE: Developers, PLEASE do not start this script and start using the 
#production database!!!!
play-1.2.5/play run --%qa -Xmx1024M 
#-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044
