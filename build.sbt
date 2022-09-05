ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

val otelVersion = "1.17.0"
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
    libraryDependencies ++= "com.google.cloud" % "google-cloud-spanner" % "6.29.0" % "test" ::
      "io.opentelemetry" % "opentelemetry-sdk" % otelVersion % "test" ::
      "com.typesafe.play" %% "play-json" % "2.8.2" % "test" ::
      "org.scalatestplus" %% "mockito-4-5" % "3.2.12.0" % "test" ::
      "org.scalatest" %% "scalatest" % "3.2.12" % "test" ::
      Nil
  )
  .dependsOn(http)
