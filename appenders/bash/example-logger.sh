#!/usr/bin/env bash
# This script will print info and error messages to the Pulse Log Collector.
# Modify these variables for your own envirinmnet:
export PULSE_COLLECTOR_HOST=localhost
export PULSE_COLLECTOR_PORT=9001
CWD="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source $CWD/logger.sh

if [ "$#" -ne 2 ]; then
    numEvents=10000
    sleepTime=1
else
    numEvents=$1
    sleepTime=$2
fi

i=0
while [ $i -lt $numEvents ]
    do
    logger --category=category --level=INFO --message=Info --threadName=1 --application=pulse-test-default
    if (( $i % 300 == 0 ))
    then
        logger --category=category --level=ERROR --message=Error --threadName=1 --application=pulse-test-default
    fi
    sleep $sleepTime
    i=$[$i+1]
done
