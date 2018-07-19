
# Java log appenders

## Log4j 1.x appenders
The log4j 1.x http appender will send a post message to the `log-collector` process with the
contents of your log message. The appender will batch messages into groups of 1000 records and will 
flush when a group is available, 1 second has passed. The appender will always flush all messages
as soon as an ERROR level message is logged.

## Special Handling 
- MDC: Fields stored in the MDC (or Mapped Diagnostic Context) will be added as a top level searchable
 field.  
- Message Properties: Fields found in the message properties will be added as a top level searchable
field. 
- NDC: The NDC (or Nested Diagnostic Context) is currently ignored

## Example log4j configuration
Here is an example log4j configuration file:

```bash
log4j.rootLogger=info, stdout, http
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%5p [%t] (%F:%L) - %m%n

log4j.appender.http=io.phdata.pulse.log.HttpAppender
log4j.appender.http.Address=http://edge2.valhalla.phdata.io:9015/v2/events/pulse-test-100
log4j.appender.http.layout=org.apache.log4j.core.layout.JsonLayout
log4j.appender.http.layout.compact=false
log4j.appender.http.layout.complete=true
# Info messages will cause the http client to recursively call the logger 
# when the connection to the log-collector is not available
log4j.logger.io.phdata.pulse.shade.org.apache.http=off
log4j.logger.io.phdata.pulse.shade.org.apache.wire=off

```

It's recommended (for now) that the logs be written to file in addition to Pulse through the http 
appender. This appender currently doesn't currently make any availability guarantees.

Example usage:

```bash
$ java -cp my-jar.jar:log-appender-{version}.jar -Dlog4j.configuration=file:log4j.properties com.my.main.class
```

(Assuming the above log4j example is named `log4j.properties`)

When initially configuring the logger, it can be helpful to put log4j in debug mode by adding
`-D log4j.debug=true` to your java command string.

## Endpoints

`/log?application=<application` (POST) DEPRECATED
Accepts a single JSON event at a time and will insert into the log indexes of `<application>`

`/v2/event/<application>` (POST)
Accepts a single JSON event at a time and will insert into the log indexes of `<application>`

`/v2/events/<application>` (POST)
Accepts a JSON array of events at a time and will insert into the log indexes of `<application>`


Sample JSON event:

```json
  {
  "category":"io.phdata.pulse.log.HttpAppenderTest",
  "timestamp":1521575572598,
  "level":"INFO",
  "message":"Hello, World",
  "threadName":"main",
  "ndc":"ndc",
  "properties":{"key":"value"},
  "thrown":["java.lang.Throwable: Test","\tat io.phdata.pulse.log.HttpAppenderTest.testRenderJson(HttpAppenderTest.java:24)","\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)","\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)","\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)","\tat java.lang.reflect.Method.invoke(Method.java:498)","\tat org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)","\tat org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)","\tat org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)","\tat org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)","\tat org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)","\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)","\tat org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)","\tat org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)","\tat org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)","\tat org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)","\tat org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)","\tat org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)","\tat org.junit.runners.ParentRunner.run(ParentRunner.java:363)","\tat org.junit.runner.JUnitCore.run(JUnitCore.java:137)","\tat com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:68)","\tat com.intellij.rt.execution.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:47)","\tat com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:242)","\tat com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:70)"]
  }
```

## Configuration for Standalone Application
Add `log-appender-{version}.jar` to your classpath

## Installation for use with Spark
To install the log4j appender for use with Apache Spark, the `log-appender-{version}.jar`  needs
to be added to the classpath in `spark-env.sh`

To modify the classpath in Cloudera, add this line to the `spark-env.sh` safety valve and redeploy
client configuration:

```bash
export SPARK_DIST_CLASSPATH="$SPARK_DIST_CLASSPATH:/opt/cloudera/parcels/PULSE/lib/appenders/*"
```
