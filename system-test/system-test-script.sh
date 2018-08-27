#!/usr/bin/env bash
# Stopping the script if any command fails
set -e
source bin/env.sh
function cleanup {
  echo "Removing /tmp/pulse-system-test"
  rm  -r /tmp/pulse-system-test
}
trap cleanup EXIT
mkdir /tmp/foo

export collection_roller_log="system-test/log_files/collectionroller.log"
export alert_engine_log="system-test/log_files/alertengine.log"
export log_collector_log="system-test/log_files/logcollector.log"

echo "Starting collection roller....."
bin/collection-roller 2>&1 > $collection_roller_log
# Getting the process id of collection roller
collection_roller_pid=$!
# Checking if the collection roller service has started
if [ -z "$collection_roller_pid" ]
then
      echo "Collection Roller is not running"
      exit 1
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
fi
echo "Starting log collector...."
bin/log-collector 2>&1 > $log_collector_log
# Getting process of log collector
log_collector_pid=$!
# Checking if the log collector service has started
if [ -z "log_collector_pid" ]
then
      echo "Log collector is not running"
      exit 1
fi

./log-example/spark-logging

echo "Curling the Solr API"
# Asking for edge node credentials
read -p "Username: " USER
query_response=$(curl -i -o - --silent -X GET -u ${SOLR_USR}:${SOLR_PWD} "http://master3.valhalla.phdata.io:8983/solr/logging-pulse_latest/select?q=*%3A*&wt=json&indent=true")
http_status_collection=$(echo "$query_response" | grep HTTP |  awk '{print $2}')
echo $http_status_collection
# Checking if the collection exists and if documents are collected
if [[  "$http_status_collection" == 200 ]]; then
       if [[ "query_response" =~ "\"numFound\":0" ]]; then
                echo "Failed!"
       else
                echo "Passed!"
       fi
else
        echo "Failed"
fi
