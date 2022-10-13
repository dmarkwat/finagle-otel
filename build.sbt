ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

val otelVersion = "1.19.0"
val otelAgentVersion = "1.19.0"
val finagleVersion = "22.7.0"

lazy val core = (project in file("core"))
  .settings(
    name := "core",
    compileOrder := CompileOrder.Mixed,
    coverageEnabled := true,
    libraryDependencies ++= "io.opentelemetry" % "opentelemetry-api" % otelVersion % "provided" ::
      // pulled in via the sdk which we don't want to depend on -- sadly not part of the api
      "io.opentelemetry" % "opentelemetry-semconv" % s"$otelVersion-alpha" % "provided" ::
      "org.slf4j" % "jul-to-slf4j" % "1.7.32" ::
      //
      // test dependencies
      //
      // also need it for testing
      "io.opentelemetry" % "opentelemetry-semconv" % s"$otelVersion-alpha" % "test" ::
      Nil
  )
  .dependsOn(finagleBridge, testbed % "test")

lazy val http = (project in file("http"))
  .settings(
    name := "http",
    compileOrder := CompileOrder.Mixed,
    libraryDependencies ++= "com.twitter" %% "finagle-http" % finagleVersion ::
      "io.opentelemetry" % "opentelemetry-sdk" % otelVersion % "provided" ::
      // todo remove: for contrived tests only
      "com.google.cloud" % "google-cloud-spanner" % "6.29.1" ::
      "io.grpc" % "grpc-okhttp" % "1.48.0" ::
      "io.opentelemetry.javaagent.instrumentation" % "opentelemetry-javaagent-opentelemetry-instrumentation-api" % s"$otelVersion-alpha" ::
      // todo temporary
      "io.opentelemetry" % "opentelemetry-opencensus-shim" % s"$otelVersion-alpha" ::
      "net.bytebuddy" % "byte-buddy-dep" % "1.12.10" ::
      "net.bytebuddy" % "byte-buddy-agent" % "1.12.10" ::
      //
      // test dependencies
      //
      Nil
  )
  .dependsOn(core, testbed % "test")

lazy val integrationTest = (project in file("integration-test"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "integration-test",
    assembly / mainClass := Some("io.dmarkwat.twitter.finagle.otel.example.App"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) =>
        xs match {
          case List("services", svc @ _*) =>
            println(svc)
            MergeStrategy.concat
          case _ => MergeStrategy.discard
        }
      case x => MergeStrategy.first
    },
    libraryDependencies ++=
      // for assembly
      "io.opentelemetry" % "opentelemetry-sdk" % otelVersion ::
        "io.opentelemetry" % "opentelemetry-opencensus-shim" % s"$otelVersion-alpha" ::
        // wanted to use latest but it depends on silently-breaking changes in underlying slf4j-api (1.x -> 2.x);
        // twitter finagle depends on 1.7.x and the breakage is deadly silent
        "ch.qos.logback" % "logback-classic" % "1.2.10" ::
        // for tests
        "com.typesafe.play" %% "play-json" % "2.8.2" % "test" ::
        "org.scalatestplus" %% "mockito-4-5" % "3.2.12.0" % "test" ::
        Nil
  )
  .settings(
    buildInfoPackage := "io.dmarkwat.twitter.finale.otel.example"
  )
  .dependsOn(http, testbed % "test")

lazy val extensions = (project in file("extensions"))
  .settings(
    name := "extensions",
    Compile / compileOrder := CompileOrder.Mixed,
    libraryDependencies ++= "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure-spi" % otelVersion % "provided" ::
      //
      // test dependencies
      //
      Nil
  )
  .dependsOn(finagleBridge)

lazy val finagleBridge = (project in file("finagle-bridge"))
  .settings(
    name := "finagle-bridge",
    Compile / compileOrder := CompileOrder.Mixed,
    libraryDependencies ++= "com.twitter" %% "finagle-core" % finagleVersion :: Nil
  )

lazy val testbed = (project in file("testbed"))
  .settings(
    name := "testbed",
    libraryDependencies ++= "io.opentelemetry" % "opentelemetry-sdk" % otelVersion ::
      "org.scalatest" %% "scalatest" % "3.2.12" ::
      Nil
  )
