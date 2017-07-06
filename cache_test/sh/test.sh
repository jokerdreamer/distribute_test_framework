#!/bin/bash

JAVA_HOME=$JAVA_HOME/bin/java

if [ $1 == "-client" ] ; then
    $JAVA_HOME cache_test.Client $2 $3 $4 $5 $6 $7 $8 $9
elif [ $1 == "-monitor" ] ; then
	$JAVA_HOME cache_test.Master $2
elif [ $1 == "-worker" ] ; then
	$JAVA_HOME cache_test cache_test.Worker $2 $3
elif [ $1 == "-help" ] ; then
    echo "-client zookeeper task duration url concurrency thread workerList"
    echo "-monitor zookeeper"
    echo "-worker zookeeper dir"
    echo "-help"
else
	echo "use -help as option for detail"
fi
