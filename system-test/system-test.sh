#!/usr/bin/env bash
# Stopping the script if any command fails
set -e
source bin/env.sh

function cleanup {
  echo "Removing /tmp/pulse-system-test"
  rm  -r /tmp/pulse-system-test
  # killing services
  kill_services
  echo "Sevices are terminated/killed"
}

trap cleanup EXIT
mkdir -p /tmp/pulse-system-test

# Writing logs to local directory
export collection_roller_log="system-test/log_files/collection_roller.log"
export alert_engine_log="system-test/log_files/alert_engine.log"
export log_collector_log="system-test/log_files/log_collector.log"
export application_log="system-test/log_files/application.log"

kill_services(){
  echo "killing service by parent ID"
  PGID=$(ps -o pgid= $$)
  kill -9 -- -$PGID
  echo "kill service end"
}

echo "Starting collection roller....."
bin/collection-roller 2>&1> $collection_roller_log &

echo "Starting alert engine....."
bin/alert-engine 2>&1> $alert_engine_log &

echo "Starting log collector...."
bin/log-collector 2>&1> $log_collector_log &

while [[ `(echo >/dev/tcp/127.0.0.1/{WEBSERVER_PORT}) &>/dev/null && echo "open" || echo "close"` == 'open' ]]; do sleep 1; done

./log-example/spark-logging 2>&1 > system-test/log_files/spark-example.log

echo "Curling the Solr API"

query_response=$(curl -i -o - --silent -X GET -u ${SOLR_USR}:${SOLR_PWD} "http://master3.valhalla.phdata.io:8983/solr/logging-pulse-test_latest/select?q=*%3A*&wt=json&indent=true")
http_status_collection=$(echo "$query_response" | grep HTTP |  awk '{print $2}')
numfound=$(echo $query_response | grep "numFound" |awk -F  "," '/1/ {print $8}'|awk -F':' '{print $3}')
echo $http_status_collection

# Checking if the collection exists and if documents are collected


if [[ "$http_status_collection" == 200 ]]; then

       if [[ "$numfound" == 0 ]]; then
                echo "Records assertion in Solr collection Failed!"
                exit 1
       else
                echo "Records assertion in Solr collection Passed!"
       fi
else
        echo "Records assertion in Solr collection Failed"
        exit 1
fi
