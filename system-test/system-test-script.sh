#!/usr/bin/env bash
set -e
cd ..

echo "starting collection roller"
bin/collection-roller &> /dev/null &

echo "starting alert engine"
bin/alert-engine &> /dev/null &

echo "starting log collector"
bin/log-collector &> /dev/null &

echo "starting spark application"
cd /home/username/pulse/log-example/
./spark-logging &> /dev/null &
sleep 10 &
echo "Curing the Solr API"
var10=$(curl -su 'username' 'http://zk.host:8983/solr/logging-pulse_latest_shard1_replica1/select?q=*%3A*&wt=json&indent=true')

if [[ "$var10" =~ "\"numFound\":0" ]]; then
        echo "No Documents"
        exit 1
else
        echo "Documents collected"
        exit 0
fi

