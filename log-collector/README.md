# Log Collector

The Log Collector is an HTTP Server that listens for log events, batches them, and writes them 
into a Solr Cloud cluster

Example usage:

```bash 
    java -cp <log-collector-jar> xio.phdata.pulse.logcollector.LogCollector \
    --port $WEBSERVER_PORT \
    --zk-hosts $SOLR_ZK_QUORUM
```

