#!/usr/bin/env bash
# Stopping the script if any command fails
set -e
source bin/env.sh

function cleanup {
  echo "Removing pulse-system-test from tmp directory"
  rm  -r /tmp/pulse-system-test
  # killing log-collector, collection-roller, Alert-engine, Example application.
  echo "killing service by parent ID"
  PID=$(ps -o pgid= $$ | pgrep -u $SOLR_USR java)
  kill -9 $PID
  echo "Sevices are terminated/killed"
}

trap cleanup EXIT
mkdir -p /tmp/pulse-system-test
mkdir -p system-test/logs

# Writing logs to local directory
export collection_roller_log="system-test/logs/collection_roller.log"
export alert_engine_log="system-test/logs/alert_engine.log"
export log_collector_log="system-test/logs/log_collector.log"
export application_log="system-test/logs/spark_example.log"

echo "Starting collection roller....."
bin/collection-roller &> $collection_roller_log &

echo "Starting alert engine....."
bin/alert-engine &> $alert_engine_log &

echo "Starting log collector...."
bin/log-collector &> $log_collector_log &

while [[ `(echo >/dev/tcp/127.0.0.1/{WEBSERVER_PORT}) &>/dev/null && echo "open" || echo "close"` == 'open' ]]; do sleep 1; done

echo "Starting Spark application"
./system-test/spark-logging &> system-test/log_files/spark_example.log

echo "Curling the Solr API"
query_response=$(curl -i -o - --silent -X GET -u ${SOLR_USR}:${SOLR_PWD} "http://${HOSTNAME3}:8983/solr/logging-pulse-test_latest/select?q=*%3A*&wt=json&indent=true")
http_status_collection=$(echo "$query_response" | grep HTTP |  awk '{print $2}')
numfound=$(echo $query_response | grep "numFound" |awk -F  "," '/1/ {print $8}'|awk -F':' '{print $3}')

# Checking if the collection exists and if documents are collected
if [[ "$http_status_collection" == 200 ]]; then
       if [[ "$numfound" == 0 ]]; then
                echo "Records assertion in Solr collection Failed!"
                exit 1
       else
                echo "Records assertion in Solr collection Passed!"
                exit 0
       fi
else
        echo "Collection does not exist"
        exit 1
fi