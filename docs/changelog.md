# 2.4.1

- Fixed NullPointerException when during log4j initialization on Cloudera CDH6. ce5a0b1352e02a898d93ab3f8f1994f78f30baf8

# 2.4.0

- Full support of CDH6.  The Pulse alert-engine and collection-roller configuration files are 
fully backwards compatible in this release. There is a breaking change in the CDH upgrade from 
Solr 4 to Solr 7 meaning the indexes are not compatible. New collection configurations are provided
 for Solr 7 and will be automatically used. Old collections should be saved, converted (or
truncated if they are not important) when moving to Pulse 2.4.0.
- Add a 'metrics' endpoint and Python library to call it that writes metrics to Kudu tables. 
See `docs/metrics.md` for details.
- Pulse can now read messages from a Kafka topic. This means that existing log shippers
 like LogStash or FluentD can now be used with Pulse.  This feature is not well documented, 
 if you plan to use this feature please contact the development team.

Note: This release has not been tested fully on CDH5

# 2.3.2 

- Build parcel for sles 12 SUSE linux

# 2.3.1
- Remove max instance restriction on the log collector role

# 2.3.0

- New documentation at <https://pulse-logging.readthedocs.io/en/latest/?>
- Fix shared threadpool that could cause the http server to block if Solr is backed up
- Use 'String' fields rather than 'text_general' for small fields like 'level' that 
don't need to be tokenized
- Set 'blocking' to 'false' by default in the log4j HttpAppender. This means that if
the Log Collector, or more likely Solr, get backed up, messages will be dropped rather
than affecting the performance of the application. This can be changed by setting 
`log4j.appender.http.blocking=true`
- Alert Engine and Collection Roller use a single connection to Zookeeper, reducing
log verbosity
- Collection Roller will no longer update Solr config set dirs every 5 minutes.
A restart of the Collection Roller is now required to update configurations.
- Add `hostname` to the default schema
- Slim down the default Solr config set dirs, removing functionality we aren't using
- Added an experimental `Metrics` class for application metrics and profiling
- HttpAppender buffer size can now be set using all lowercase letters `log4j.appender.http.buffersize`
- Add a secure Solr config set dir called `pulseconfig-securev1`
- Add new configuration options to the Log Collector:
  - The log collector will batch up messages over a period of time before sending them to Solr.
  - The batch size, duration, and max buffer size are configurable via java options:
    - `pulse.collector.stream.buffer.max`: Max number of messages to be kept in the log collector buffer
before new messages are dropped.  This buffer can grow and become full if Solr is overloaded. Default is 512000 messages
    - `pulse.collector.stream.batch.size`: Size of the batches to send to solr. Default is 1000 messages
    - `pulse.collector.stream.flush.seconds`: Number of seconds to wait before flushing documents to solr. Default is 1 second
    - `pulse.collector.stream.overflow.strategy`: [OverflowStrategy](https://doc.akka.io/japi/akka/current/akka/stream/OverflowStrategy.html) 
    to employ if the buffer has reached its max size. Options are 'dropnew', 'drophead', 'droptail', 'backpressure', and 'dropbuffer'. Defaults to `fail`

# 2.2.0

- Don't require 'kudu masters' in Cloudera Manager service
- Add default alert and collection roller yaml configs for Cloudera Manager Service first run
- Require 'solrConfigSetDir' in collection-roller.yml to be set and populated
- Fix parcel shasums in parcel repo
- Add missing backslash in spark-submit example script

# 2.1.0

- Log4j appender detects Spark application and will write 'application_id' and 'container_id' to the Solr index
- Add template to create and import Arcadia dashboard
- Add ansible playbook for deploying CSD
- Better logging in the CollectionRoller

# 2.0.2

- Fix issue where Solr collections would be deleted too early

# 2.0.1

- Fix email authentication for non authenticated smtp servers

