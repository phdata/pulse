# Collection Roller
The collection roller will first create an application if it does not exist. An application consists
of 
Logs are kept for a limited amount of time (weeks or months). Logs for individual applications will
actually be multiple collections, each covering a time period.
- A series of collections, one created for each day the application is running (appname_date)
- Latest collection alias (appname_latest): This alias points at a single collection that takes new log 
messages
- All collections alias (appname_all): This alias points at all non-retired logging collections

## Rolling
After an application has been created, the collection roller will create new collections for each
day and delete collections older than the configured `numCollections` hold limit.

## Configuration

A Collection Roller configuration file is written in yaml and will look like:

```yaml

sorlConfigSetDir: /etc/pulse/solr-configs/ # directoring containing one or many solr instancedir configs to be uploaded. The name of the config when uploaded to solr will be the name of the directory
applications:
# Config using all defaults
- name: pulse-test-default
  solrConfigSetName: pulseconfigv2
# Config using options
- name: pulse-test-options
  numCollections: 7 # (optional) number of collections to keep at any given time. Defaults to 7
  shards: 1 # (optional) solr collection shards, defaults to 1
  replicas: 1 # (optional) number of solr config replicas. Defaults to 1
  rollPeriod: 1 # (optional) number of days before the collection is rolled
  solrConfigSetName: pulseconfigv2 # name of solr config to use for the collections

```

This configuration file is passes as a CLI argument along with a list of zookeeper hosts.

## Running the app
see `scripts/run-example`
