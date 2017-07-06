#!/bin/bash


if [ $1 == "-client" ] ; then
    java -cp zookeeper-3.4.6.jar:cache_test.jar:slf4j-log4j12-1.7.7.jar:slf4j-api-1.7.5.jar:log4j-1.2.15.jar cache_test.Client $2 $3 $4 $5 $6 $7 $8 $9 $10
elif [ $1 == "-monitor" ] ; then
    java -cp zookeeper-3.4.6.jar:cache_test.jar:slf4j-log4j12-1.7.7.jar:slf4j-api-1.7.5.jar:log4j-1.2.15.jar cache_test.Master $2
elif [ $1 == "-worker" ] ; then
    java -cp zookeeper-3.4.6.jar:cache_test.jar:slf4j-log4j12-1.7.7.jar:slf4j-api-1.7.5.jar:log4j-1.2.15.jar cache_test.Worker $2 $3
elif [ $1 == "-help" ] ; then
    echo "-client zookeeper task duration url concurrency thread workerList script"
    echo "-monitor zookeeper"
    echo "-worker zookeeper dir"
    echo "-help"
else
    echo "use -help as option for detail"
fi
