#!/usr/bin/env bash
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
    logger --category=category --level=INFO --message=Info --threadName=1
    if (( $numEvents % 300 == 0 ))
    then
        logger --category=category --level=ERROR --message=Error --threadName=1
    fi
    sleep $sleepTime
    i=$[$i+1]
done