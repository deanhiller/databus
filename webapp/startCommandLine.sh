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
