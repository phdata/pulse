#!/usr/bin/env bash
# Stopping the script if any command fails
set -euo pipefail

export collection_roller_log="system-test/collection-roller_logfile"
export alert_engine_log="system-test/alert-engine_logfile"
export log_collector_log="system-test/log-collector_logfile"
export spark_log="system-test/spark_logfile"

echo "Starting collection roller....."
bin/collection-roller 2>&1 > $collection_roller_log &
# Getting the process id of collection roller
collection_roller_pid=$!
# Checking if the collection roller service has started
if [ -z "$collection_roller_pid" ]
then
      echo "Collection Roller is not running"
      exit 1
else
      echo "Collection Roller is running....."
      #exit 0
fi
echo "Starting alert engine....."
bin/alert-engine 2>&1 > $alert_engine_log &
# Getting process of alert engine
alert_engine_pid=$!
# Checking if the alert engine service has started
if [ -z "$alert_engine_pid" ]
then
      echo "Alert Engine is not running"
      exit 1
else
      echo "Alert Engine is running...."
      #exit 0
fi
echo "Starting log collector...."
bin/log-collector 2>&1 > $log_collector_log &
# Getting process of log collector
log_collector_pid=$!
# Checking if the log collector service has started
if [ -z "log_collector_pid" ]
then
      echo "Log collector is not running"
      exit 1
else
      echo "Log Collector is running...."
      #exit 0
fi

cd /home/mgeorge/pulse/log-example/
./spark-logging 2>&1 > $spark_log &

echo "Curling the Solr API"
# Asking for edge node credentials
read -p "Username: " USER
query_response=$(curl -i -X GET \
                   'http://master3.valhalla.phdata.io:8983/solr/logging-pulse_latest/select?q=%2A%3A%2A&wt=json&indent=true' \
                   -H 'Authorization: Basic bWdlb3JnZTp3czJlZDNyZjQh' \
                   -H 'Cache-Control: no-cache' \
                   -H 'Postman-Token: cc1420a0-9efc-4227-9cb8-be129a9ebd94')
http_status_collection=$(echo "$query_response" | grep HTTP |  awk '{print $2}')
echo $http_status_collection
# Checking if the collection exists and if documents are collected
if [[  "$http_status_collection" == 200 ]]; then
       #echo "Passed"
       if [[ "query_response" =~ "\"numFound\":0" ]]; then
                echo "Failed!"
       else
                echo "Passed!"
       fi
else
        echo "Failed"
fi

# Killing service PIDs
echo "Killing service PIDS"
kill -9 $log_collector_pid
kill -9 $collection_roller_pid
kill -9 $alert_engine_pid