import sbt.Keys._
import sbt._
import sys.process._

name := "pulse"
organization in ThisBuild := "io.phdata"
scalaVersion in ThisBuild := "2.11.12"
lazy val projectVersion = scala.util.Properties.envOrElse("VERSION", "SNAPSHOT")
lazy val kuduClassifier = scala.util.Properties.envOrElse("KUDU_BINARY_CLASSIFIER", "osx-x86_64")

val kuduMessage = println(s"Using Kudu Binary: $kuduClassifier")

concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)
val CDH5 = 5
val CDH6 = 6

val cdhVersion = scala.util.Properties.envOrElse("CDH_VERSION", "6").toInt

lazy val compilerOptions = Seq(
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-deprecation",
  "-encoding",
  "utf8"
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  version := projectVersion,
  resolvers ++= Seq(Resolver.sonatypeRepo("releases"),
                    "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
                    "restlet" at "http://maven.restlet.com")
)

lazy val assemblySettings = Seq(
  test in assembly := {},
  assemblyShadeRules in assembly := Seq(
    ShadeRule.rename("org.apache.http.**" -> "io.phdata.pulse.shade.org.apache.http.@1").inAll
  )
)

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true,
    scalafmtTestOnCompile := true,
    scalafmtVersion := "1.2.0"
  )

lazy val dependencies =
  new {
    // Common depends
    val logback        = "ch.qos.logback"             % "logback-classic" % logbackVersion
    val scalaLogging   = "com.typesafe.scala-logging" %% "scala-logging"  % scalaLoggingVersion
    val commonsLogging = "commons-logging"            % "commons-logging" % "1.2"
    val commonsCodec   = "commons-codec"              % "commons-codec"   % "1.9"
    val slf4jApi   = "org.slf4j"                  % "slf4j-api"   % "1.7.5"

    // CLI parsing depends
    val scallop   = "org.rogach"    %% "scallop"      % scallopVersion
    val scalaYaml = "net.jcazevedo" %% "moultingyaml" % scalaYamlVersion

    val sprayJson = "io.spray" %% "spray-json" % sprayJsonVersion

    val spark = "org.apache.spark" %% "spark-core" % sparkVersion % Provided
    // Testing depends
    val scalaTest         = "org.scalatest" %% "scalatest"                   % scalaTestVersion     % Test
    val scalaDockerTest   = "com.whisk"     %% "docker-testkit-scalatest"    % dockerTestKitVersion % Test
    val spotifyDockerTest = "com.whisk"     %% "docker-testkit-impl-spotify" % dockerTestKitVersion % Test
    // Needed for Solr Minicluster tests
    val slf4jLog4j   = "org.slf4j"                  % "slf4j-log4j12"   % "1.7.5" % Test


    val kudu          = "org.apache.kudu" % "kudu-client"     % kuduVersion
    val kuduTestUtils = "org.apache.kudu" % "kudu-test-utils" % kuduVersion % Test
    // TODO classifier make compatible with build server for when we re-enable integration tests on Travis
    val kuduBinary = "org.apache.kudu" % "kudu-binary" % kuduVersion % Test classifier kuduClassifier

    val mockito = "org.mockito" % "mockito-all" % mockitoVersion % Test

    val powermock      = "org.powermock" % "powermock-module-junit4" % powerMockVersion % Test
    val powerMockJunit = "org.powermock" % "powermock-module-junit4" % powerMockVersion % Test
    val powerMockApi   = "org.powermock" % "powermock-api-mockito"   % powerMockVersion % Test

    // Kafka depends
    val apacheKafka = "org.apache.kafka" % "kafka_2.11" % "0.10.2-kafka-2.2.0" % Provided

    // Http depends
    val akkaActor         = "com.typesafe.akka" %% "akka-actor"           % akkaActorVersion
    val akkaStream        = "com.typesafe.akka" %% "akka-stream"          % akkaActorVersion
    val akkaHttp          = "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion
    val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion

    val akkaTestKit  = "com.typesafe.akka" %% "akka-testkit"      % "2.4.20"        % Test
    val akkaHttpTest = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test

    val solrj4    = "org.apache.solr" % "solr-solrj"          % solrj4Version exclude ("org.slf4j", "*")
    val solr4Test = "org.apache.solr" % "solr-test-framework" % solrj4Version % Test exclude ("org.slf4j", "*")

    val solrj7    = "org.apache.solr" % "solr-solrj"          % solrj7Version exclude ("org.slf4j", "*")
    val solr7Test = "org.apache.solr" % "solr-test-framework" % solrj7Version % Test exclude ("org.slf4j", "*")

    val solr4 = Seq(solrj4, commonsCodec, solr4Test)
    val solr7 = Seq(solrj7, commonsCodec, solr7Test)

    val solr = cdhVersion match {
      case CDH5 => solr4
      case CDH6 => solr7
    }

    //Alert Engine
    val javaMail = "javax.mail" % "mail" % javaMailVersion

    // log-appender
    val log4j           = "log4j"                      % "log4j"            % log4jVersion % Provided
    val httpClient      = "org.apache.httpcomponents"  % "httpclient"       % httpClientVersion
    val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
    val jacksonCore     = "com.fasterxml.jackson.core" % "jackson-core"     % jacksonVersion

    val junit          = "junit"         % "junit"           % junitVersion % Test
    val junitInterface = "com.novocode"  % "junit-interface" % "0.11" % Test
    val cats           = "org.typelevel" %% "cats-core"      % "1.1.0"
    val monix          = "io.monix"      %% "monix"          % monixVersion

    val common = Seq(scalaLogging, scalaTest, slf4jApi, slf4jLog4j, commonsLogging, cats)

    val cli = Seq(scallop, scalaYaml)
    val all = common ++ cli ++ Seq(scalaDockerTest, spotifyDockerTest)

    val http = Seq(akkaHttp, akkaHttpSprayJson, akkaTestKit, akkaHttpTest, akkaActor, akkaStream)

    val mocking = Seq(mockito, powermock, powerMockApi, powerMockJunit)
  }

lazy val settings = commonSettings ++ scalafmtSettings ++ assemblySettings

val solrModule = cdhVersion match {
  case CDH5 =>
    println(s"** Compiling with Solr 4 **")
    solr4
  case CDH6 =>
    println(s"** Compiling with Solr 7 **")
    solr7
  case _ => throw new Exception("CDH_VERSION not recognized, should be one of '5,6''")
}

lazy val `log-appender` = project
  .settings(
    name := "log-appender",
    settings,
    libraryDependencies ++= Seq(
      dependencies.log4j,
      dependencies.httpClient,
      dependencies.jacksonDatabind,
      dependencies.jacksonCore,
      dependencies.junit,
      dependencies.junitInterface,
      dependencies.scalaTest
    ) ++ dependencies.mocking
  )
  .enablePlugins(JavaServerAppPackaging)

lazy val `log-collector` = project
  .settings(
    name := "log-collector",
    mainClass in Compile := Some("io.phdata.pulse.logcollector.LogCollector"),
    settings,
    libraryDependencies ++= dependencies.http ++ dependencies.mocking ++ Seq(
      dependencies.scallop,
      dependencies.apacheKafka,
      dependencies.monix,
      dependencies.kudu,
      dependencies.kuduBinary,
      dependencies.kuduTestUtils
    ) ++ dependencies.solr ++ dependencies.common
  )
  .dependsOn(common)
  .dependsOn(solrModule)
  .dependsOn(solrModule % "test->test")

lazy val `collection-roller` = project
  .settings(
    name := "collection-roller",
    mainClass in Compile := Some("io.phdata.pulse.logcollector.roller.CollectionRollerMain"),
    settings,
    libraryDependencies ++= dependencies.common
    ++ Seq(dependencies.scalaYaml, dependencies.scallop) ++ dependencies.solr
  )
  .dependsOn(common)
  .dependsOn(solrModule)
  .dependsOn(solrModule % "test->test")

lazy val `alert-engine` = project
  .settings(
    name := "alert-engine",
    mainClass in Compile := Some("io.phdata.pulse.alertengine.AlertEngineMain"),
    settings,
    libraryDependencies ++= dependencies.common ++ dependencies.solr ++ Seq(dependencies.javaMail) ++ Seq(
      dependencies.scalaYaml,
      dependencies.scallop) ++ dependencies.mocking
  )
  .dependsOn(common)
  .dependsOn(solrModule)
  .dependsOn(solrModule % "test->test")

lazy val `common` = project
  .settings(name := "common", settings, libraryDependencies ++= dependencies.common)

lazy val `solr4` = project
  .settings(
    name := "solr4",
    settings,
    libraryDependencies ++= dependencies.common ++ dependencies.solr4 ++ Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion
    )
  )
  .dependsOn(common)

lazy val solr7 = project
  .settings(
    name := "solr7",
    settings,
    libraryDependencies ++= (dependencies.common ++ dependencies.solr7 ++ Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion
    ))
  )
  .dependsOn(common)

lazy val `log-example` = project
  .settings(
    name := "log-example",
    settings,
    libraryDependencies ++= Seq("log4j" % "log4j" % "1.2.16",
                                dependencies.spark,
                                dependencies.scalaTest)
  )
  .dependsOn(`log-appender`)

// Library versions
val logbackVersion       = "1.2.3"
val scalaLoggingVersion  = "3.7.2"
val scallopVersion       = "3.1.5"
val scalaYamlVersion     = "0.4.0"
val scalaTestVersion     = "3.0.8"
val dockerTestKitVersion = "0.9.5"
val solrj4Version        = "4.10.3-cdh5.12.1"
val solrj7Version        = "7.4.0-cdh6.1.1"
val akkaActorVersion     = "2.5.23"
val akkaHttpVersion      = "10.1.10"
val sprayJsonVersion     = "1.3.5"
val sparkVersion         = "2.2.0.cloudera1"
val log4jVersion         = "1.2.17"
val httpClientVersion    = "4.5.5"
val jacksonVersion       = "2.9.4"
val junitVersion         = "4.12"
val javaMailVersion      = "1.4"
val mockitoVersion       = "1.10.19"
val powerMockVersion     = "1.6.6"
val monixVersion         = "2.3.3"
val kuduVersion          = "1.10.0"
val catsCoreVersion      = "1.6.0"
