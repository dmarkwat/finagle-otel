plugins {
    id("io.dmarkwat.twitter.finagle.scala-library-conventions")
    id("com.ryandens.javaagent-test") version "0.4.0"
}

dependencies {
    javaagent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.19.1")

    testImplementation("io.opentelemetry:opentelemetry-sdk")

    testImplementation(project(":testbed"))
    testImplementation(project(":core"))
    testImplementation(project(":core:testing:lib"))
}
