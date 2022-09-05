ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

val otelVersion = "1.17.0"
val otelAgentVersion = "1.17.0"
val finagleVersion = "22.7.0"

lazy val core = (project in file("core"))
  .settings(
    name := "core",
    libraryDependencies ++= "com.twitter" %% "finagle-core" % finagleVersion ::
      "io.opentelemetry" % "opentelemetry-api" % otelVersion % "provided" ::
      // pulled in via the sdk which we don't want to depend on -- sadly not part of the api
      "io.opentelemetry" % "opentelemetry-semconv" % s"$otelVersion-alpha" % "provided" ::
      //
      // test dependencies
      //
      // also need it for testing
      "io.opentelemetry" % "opentelemetry-semconv" % s"$otelVersion-alpha" % "test" ::
      "org.scalatest" %% "scalatest" % "3.2.12" % "test" ::
      Nil
  )

lazy val http = (project in file("http"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "http",
    libraryDependencies ++= "com.twitter" %% "finagle-http" % finagleVersion ::
      "io.opentelemetry" % "opentelemetry-sdk" % otelVersion % "provided" ::
      //
      // test dependencies
      //
      "io.opentelemetry" % "opentelemetry-sdk" % otelVersion % "test" ::
      "org.scalatest" %% "scalatest" % "3.2.12" % "test" ::
      Nil,
    buildInfoPackage := "io.dmarkwat.twitter.finale.tracing.otel",
    buildInfoKeys ++= Seq[BuildInfoKey](
      "finagleVersion" -> finagleVersion
    )
  )
  .dependsOn(core)

lazy val integrationTest = (project in file("integration-test"))
  .settings(
    name := "integration-test",
    assembly / mainClass := Some("io.dmarkwat.twitter.finagle.otel.example.App"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x                             => MergeStrategy.first
    },
    libraryDependencies ++=
      // for assembly
      "io.opentelemetry" % "opentelemetry-sdk" % otelVersion ::
        // wanted to use latest but it depends on silently-breaking changes in underlying slf4j-api (1.x -> 2.x);
        // twitter finagle depends on 1.7.x and the breakage is deadly silent
        "ch.qos.logback" % "logback-classic" % "1.2.10" ::
        // for tests
        "com.google.cloud" % "google-cloud-spanner" % "6.29.0" % "test" ::
        "io.opentelemetry" % "opentelemetry-sdk" % otelVersion % "test" ::
        "com.typesafe.play" %% "play-json" % "2.8.2" % "test" ::
        "org.scalatestplus" %% "mockito-4-5" % "3.2.12.0" % "test" ::
        "org.scalatest" %% "scalatest" % "3.2.12" % "test" ::
        Nil
  )
  .dependsOn(http)

lazy val extensions = (project in file("extensions"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "extensions",
    Compile / compileOrder := CompileOrder.Mixed,
    libraryDependencies ++= "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure-spi" % otelVersion % "provided" ::
      //
      // test dependencies
      //
      "io.opentelemetry" % "opentelemetry-sdk" % otelVersion % "test" ::
      "org.scalatest" %% "scalatest" % "3.2.12" % "test" ::
      Nil,
    buildInfoPackage := "io.dmarkwat.twitter.finagle.tracing.otel.ext",
    buildInfoRenderFactory := sbtbuildinfo.JavaSingletonRenderer.apply,
    buildInfoKeys ++= Seq[BuildInfoKey](
      "finagleVersion" -> finagleVersion
    )
  )
