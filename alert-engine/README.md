# Solr Alert Engine
An alert engine will run pre-defined queries against application logs, alerting via email if they
return a number of results above or below some threshold. For example, a query could be if an
application has had any error messages in the past hour.

The alert engine will have
- User defined queries
- Send emails to a list of email addresses per query

## Config file
Here's an example of an Alert Engine configuration file:


```yaml

---
applications:
- name: pulse-test-100
  alertRules:
  - query: "timestamp:[NOW-10MINUTES TO NOW] AND level: ERROR"
    retryInterval: 10
    resultThreshold: 0 # If the threshold is set to `-1` it will throw an alert if no results are returned
    alertProfiles:
    - mailProfile1
  emailProfiles:
  - name: mailProfile1
    addresses:
    - address@company.com
  slackProfiles:
  - name: slackProfile1
    url: testurl.com

```
The alert engine takes CLI arguments including a configuration file and zookeeper hosts - because we are using CloudSolrServer to connect to Solr. The solr nodes are stored in Zookkeeper, and this gives things like load balancing for free from the client.
The top-level object of the configuration file is a list of applications.
An application consists of two things, alerts and profiles. Alerts tell the AlertEngine what to alert on. Profiles are used to set up connections to services like email or chat clients. As many profiles can be defined as needed, so you can send alerts through both email and Slack, or any number of services.
An alert rule consists of

- query: the query is a lucene syntax search query. If the query returns true, an alert is triggered.
- retryInterval: the query will be run on the retry interval. The retry interval is set in minutes
- threshold: if the query returns more than threshold results, an alert will be triggered. The default is 0. If the threshold is set to `-1`, the non-existence of documents with this query will trigger an alert. This is useful for tracking application uptime.
- alertProfiles: One or many alertProfiles can be defined. For each alertProfile defined in an alert, an alertProfile needs to be defined for the application

