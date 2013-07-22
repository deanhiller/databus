#!/bin/bash

export TEMP=
for f in lib/*.jar
do
#    echo "jarfile=$f"
    TEMP=$TEMP:$f
done

CLASSPATH=${TEMP:1}
export CLASSPATH
#echo classpath=$CLASSPATH

java -cp $CLASSPATH com.alvazan.ssql.cmdline.PlayOrm $@ 
#java -cp $CLASSPATH -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044 com.alvazan.ssql.cmdline.PlayOrm $@ 
