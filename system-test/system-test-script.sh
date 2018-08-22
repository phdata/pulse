#!/usr/bin/env bash
#Stopping the script if any command fails
set -euo pipefail
export myvar="system-test/logfile"
cd ..
 echo "Starting collection roller....."
bin/collection-roller 2>&1 > $myvar &
#Getting the process id of collection roller
pid1=$!
#Checking if the collection roller service has started
if [ -z "$pid1" ]
then
      echo "Collection Roller is not running"
else
      echo "Collection Roller is running....."
fi
echo "Starting alert engine....."
bin/alert-engine 2>&1 > $myvar &
#Getting process of alert engine
pid2=$!
#Checking if the alert engine service has started
if [ -z "$pid2" ]
then
      echo "Alert Engine is not running"
else
      echo "Alert Engine is running...."
fi
echo "Starting log collector...."
bin/log-collector 2>&1 > $myvar &
#Getting process of log collector
pid3=$!
#Checking if the log collector service has started
if [ -z "$pid3" ]
then
      echo "Log collector is not running"
else
      echo "Log Collector is running...."
fi
echo "Collection roller PID: " $pid1
echo "Log Collector PID:" $pid3
echo "Alert Engine PID:" $pid2
echo "Starting spark application...."
cd /home/mgeorge/pulse/log-example/
./spark-logging 2>&1 > $myvar &
sleep 10 &
echo "Curling the Solr API"
#Asking for edge node credentials
read -p "Username: " CURLUSER
response=$(curl -su ${CURLUSER} 'http://master3.valhalla.phdata.io:8983/solr/logging-pulse_latest/select?q=*%3A*&wt=json&indent=true')
#Checking if the collection exists and if documents are collected
if [[ $(curl -su ${CURLUSER} 'http://master3.valhalla.phdata.io:8983/solr/logging-pulse_latest/select?q=*%3A*&wt=json&indent=true' -o /dev/null -w '%{http_code}\n' -s) == 200 ]]; then
       #echo "Passed"
       if [[ "$response" =~ "\"numFound\":0" ]]; then
                echo "Failed!"
       else
                echo "Passed!"
       fi
else
        echo "Failed"
fi
#Killing service PIDs
echo "Killing service PIDS"
sudo kill -13 $pid1
sudo kill -13 $pid2
sudo kill -13 $pid3
