#!/bin/bash

#Launch the polling program...
nohup java -cp "../conf:../lib/*" gov.nrel.modbusclient.ModBusClient ../conf/meters.csv ../conf/ModBusClient.properties ../conf/meterModel.json 2>&1 >../logs/out.log
