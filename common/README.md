# Solr Alert Engine
An alert engine will run pre-defined queries against application logs, alerting via email if they
return a number of results above or below some threshold. For example, a query could be if an
application has had any error messages in the past hour.

The alert engine will have
- User defined queries
- Send emails to a list of email addresses per query

An example configuration for an alert might look like:

```yaml
- application_name: app1
  alerts:
  - name: alert when there are any errors
    query: "logLevel: \"ERROR\"”
    threshold: 0
            period: 5 minutes
    mailing_lists
    - name: phdata
        email: data-ops@phdata.io
    - name: customer1
        email: jim@customer.com
```

The alert engine will loop through all alert queries and run them according configured time
period
