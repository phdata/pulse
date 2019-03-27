import sbt.Keys._
import sbt._

name := "pulse"
organization in ThisBuild := "io.phdata"
scalaVersion in ThisBuild := "2.11.11"
lazy val projectVersion = { "bash ./version" !! }.trim

concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

lazy val integrationTests = config("it") extend Test

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
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
    "restlet" at "http://maven.restlet.com" // what is this? Couldn't resolve "module not found: org.restlet.jee". There's gotta be another way to get this.
  )
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

    // CLI parsing depends
    val scallop   = "org.rogach"    %% "scallop"      % scallopVersion
    val scalaYaml = "net.jcazevedo" %% "moultingyaml" % scalaYamlVersion

    val sprayJson = "io.spray" %% "spray-json" % sprayJsonVersion

    val spark = "org.apache.spark" %% "spark-core" % sparkVersion % Provided
    // Testing depends
    val scalaTest         = "org.scalatest" %% "scalatest"                   % scalaTestVersion     % "test,it"
    val scalaDockerTest   = "com.whisk"     %% "docker-testkit-scalatest"    % dockerTestKitVersion % "test,it"
    val spotifyDockerTest = "com.whisk"     %% "docker-testkit-impl-spotify" % dockerTestKitVersion % "test,it"

    val solrj = "org.apache.solr" % "solr-solrj" % solrjVersion

    val kudu          = "org.apache.kudu" % "kudu-client"     % kuduVersion
    val kuduTestUtils = "org.apache.kudu" % "kudu-test-utils" % kuduVersion % "test,it"
    // TODO classifier make compatible with build server for when we re-enable integration tests on Travis
    val kuduBinary    = "org.apache.kudu" % "kudu-binary"     % kuduVersion % "test,it" classifier "osx-x86_64"

    val mockito = "org.mockito" % "mockito-all" % mockitoVersion % "test,it"

    val powermock      = "org.powermock" % "powermock-module-junit4" % powerMockVersion % "test,it"
    val powerMockJunit = "org.powermock" % "powermock-module-junit4" % powerMockVersion % "test,it"
    val powerMockApi   = "org.powermock" % "powermock-api-mockito"   % powerMockVersion % "test,it"

    // Kafka depends
    val apacheKafka = "org.apache.kafka" % "kafka_2.11" % "0.10.2-kafka-2.2.0" % Provided

    // Http depends
    val akkaHttp          = "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion
    val akkaHttpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
    val akkaCors          = "ch.megard"         %% "akka-http-cors"       % akkaCorsVersion

    val akkaTestKit  = "com.typesafe.akka" %% "akka-testkit"      % "2.4.20"        % "test,it"
    val akkaHttpTest = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test,it"

    // Solr Depends
    val solr = Seq(solrj, commonsCodec)

    //Alert Engine
    val javaMail = "javax.mail" % "mail" % javaMailVersion

    // log-appender
    val log4j           = "log4j"                      % "log4j"            % log4jVersion % Provided
    val httpClient      = "org.apache.httpcomponents"  % "httpclient"       % httpClientVersion
    val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
    val jacksonCore     = "com.fasterxml.jackson.core" % "jackson-core"     % jacksonVersion

    val junit          = "junit"         % "junit"           % junitVersion % "test,it"
    val junitInterface = "com.novocode"  % "junit-interface" % "0.11" % "test,it"
    val cats           = "org.typelevel" %% "cats-core"      % "1.1.0"
    val monix          = "io.monix"      %% "monix"          % monixVersion

    val common = Seq(scalaLogging, scalaTest, logback, commonsLogging, cats)
    val cli    = Seq(scallop, scalaYaml)
    val all    = common ++ cli ++ Seq(scalaDockerTest, spotifyDockerTest)

    val http = common ++ solr ++ Seq(akkaHttp,
                                     akkaHttpSprayJson,
                                     akkaCors,
                                     akkaTestKit,
                                     akkaHttpTest)

    val mocking = Seq(mockito, powermock, powerMockApi, powerMockJunit)
  }

lazy val settings = commonSettings ++ scalafmtSettings ++ assemblySettings

lazy val `log-appender` = project
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
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
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
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
    )
  )
  .dependsOn(`test-common` % "test,it")
  .dependsOn(common)

lazy val `collection-roller` = project
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    name := "collection-roller",
    mainClass in Compile := Some("io.phdata.pulse.logcollector.roller.CollectionRollerMain"),
    settings,
    libraryDependencies ++= dependencies.common
    ++ Seq(dependencies.scalaYaml, dependencies.scallop)
  )
  .dependsOn(`test-common` % "test,it")
  .dependsOn(common)

lazy val `alert-engine` = project
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    name := "alert-engine",
    mainClass in Compile := Some("io.phdata.pulse.alertengine.AlertEngineMain"),
    settings,
    libraryDependencies ++= dependencies.common ++ dependencies.solr ++ Seq(dependencies.javaMail) ++ Seq(
      dependencies.scalaYaml,
      dependencies.sprayJson,
      dependencies.scallop) ++ dependencies.mocking
  )
  .dependsOn(`test-common` % "test,it")
  .dependsOn(common)

lazy val `common` = project
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    name := "common",
    settings,
    libraryDependencies ++= dependencies.common ++ dependencies.solr ++ dependencies.http
  )
  .dependsOn(`test-common` % "test,it")

lazy val `test-common` = project
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    name := "test-common",
    settings,
    libraryDependencies ++= dependencies.common ++ dependencies.solr ++ Seq(
      "commons-logging" % "commons-logging"     % "1.2",
      "org.apache.solr" % "solr-test-framework" % solrTestFrameworkVersion,
      "org.scalatest"   %% "scalatest"          % scalaTestVersion
    )
  )

lazy val `log-example` = project
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    name := "log-example",
    settings,
    libraryDependencies ++= Seq("log4j" % "log4j" % "1.2.16",
                                dependencies.spark,
                                dependencies.scalaTest)
  )
  .dependsOn(`log-appender`)

// Library versions
val logbackVersion           = "1.2.3"
val scalaLoggingVersion      = "3.7.2"
val scallopVersion           = "3.1.1"
val scalaYamlVersion         = "0.4.0"
val scalaTestVersion         = "3.0.4"
val dockerTestKitVersion     = "0.9.5"
val solrjVersion             = "4.10.3-cdh5.12.1"
val solrTestFrameworkVersion = "4.10.3-cdh5.12.1"
val akkaHttpVersion          = "10.0.11"
val akkaCorsVersion          = "0.2.2"
val sprayJsonVersion         = "1.3.3"
val sparkVersion             = "2.2.0.cloudera1"
val log4jVersion             = "1.2.17"
val httpClientVersion        = "4.5.5"
val jacksonVersion           = "2.9.4"
val junitVersion             = "4.12"
val javaMailVersion          = "1.4"
val mockitoVersion           = "1.10.19"
val powerMockVersion         = "1.6.3"
val monixVersion             = "2.3.3"
val kuduVersion              = "1.9.0"
