2.2.0
- Don't require 'kudu masters' in Cloudera Manager service
- Add default alert and collection roller yaml configs for Cloudera Manager Service first run
- Require 'solrConfigSetDir' in collection-roller.yml to be set and populated.
- Fix parcel shasums in parcel repo
- Add missing backslash in spark-submit example script
2.1.0
- Log4j appender detects Spark application and will write 'application_id' and 'container_id' to the Solr index
- Add template to create and import Arcadia dashboard
- Add ansible playbook for deploying CSD
- Better logging in the CollectionRoller

2.0.2
- Fix issue where Solr collections would be deleted too early
2.0.1
- Fix email authentication for non authenticated smtp servers

