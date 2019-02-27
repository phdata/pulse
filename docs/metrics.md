# Metrics

**Note**: This functionality is experimental.

(TODO put the `log-appender` jar in Artifactory)

To use the `Metrics` class described below, add the `log-appender` jar to your project.

Pulse comes with some utility methods for writing out metrics (such as performance metrics or
profiling) to your log files. These metrics can then be used in an Alert rule or graphed in a
visualization tool.

Metrics are added to the MDC (Mapped Diagnostic Context) for a single log message.

Each metric is tagged with a name. The name of the metric is the name of the field in Solr.
If the field is not pre-created in Solr a dynamic field can be used to create a metric of the
right data type by appending a suffix to the tag:

`_s`: String
`_i`: Integer
`_l`: Long
`_f`: Float
`_d`: Double
(no suffix) : String

For example, a `Long` type metric can be added using the Metrics helper class like

The `Metrics` class needs to know about your specific logger because if it created one itself then
all the line numbers and info would be for the `Metrics` class, which would be misleading.

Before using the metrics class, define your logger as an `implicit` value so it will be automatically
passed into the Metrics functions.

```
private implicit val log: Logger = LoggerFactory.getLogger(this.getClass)

```
Then you can create a metric:

```
Metrics.gauge("my_metric_l", 1L)

```

If you do not define your logger implicitly, you can just pass it into the `Metrics` function:

```
val log: Logger = LoggerFactory.getLogger(this.getClass)
Metrics.gauge("my_metric_l", 1L)(log)
```

There is also a helper function for profiling function run times. This will write the tag 'timed_function_l'
with the value of the runtime of the function.:

```
val result = Metrics.time("timed_function_l") {
      Thread.sleep(10)
      "ok"
    }

```
