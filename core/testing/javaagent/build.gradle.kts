plugins {
    id("io.dmarkwat.twitter.finagle.scala-library-conventions")
    id("com.ryandens.javaagent-test") version "0.4.0"
}

val otelJavaagentVersion: String by extra.properties

dependencies {
    javaagent("io.opentelemetry.javaagent:opentelemetry-javaagent:${otelJavaagentVersion}")

    testImplementation("io.opentelemetry:opentelemetry-sdk")

    testImplementation(project(":testbed"))
    testImplementation(project(":core"))
    testImplementation(project(":core:testing:lib"))
}
