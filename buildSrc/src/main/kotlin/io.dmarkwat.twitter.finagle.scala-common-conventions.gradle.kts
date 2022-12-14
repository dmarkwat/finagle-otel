/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    // Apply the scala Plugin to add support for Scala.
    scala
}

repositories {
    mavenLocal()
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

val otelVersion by extra("1.19.0")
val otelAlphaVersion by extra("${otelVersion}-alpha")
val otelJavaagentVersion by extra("1.19.1")
val otelJavaagentAlphaVersion by extra("${otelJavaagentVersion}-alpha")

val finagleVersion by extra("22.7.0")
val scalaVersion by extra("2.13.8")

dependencies {
    constraints {
        // Define dependency versions as constraints
        implementation("org.scala-lang:scala-library:$scalaVersion")

        implementation("org.scalatest:scalatest_2.13:3.2.12")
        implementation("junit:junit:4.13.2")
        implementation("org.scalatestplus:junit-4-13_2.13:3.2.2.0")

        implementation("io.opentelemetry:opentelemetry-api:${otelVersion}")
        implementation("io.opentelemetry:opentelemetry-sdk:${otelVersion}")
        implementation("io.opentelemetry:opentelemetry-opencensus-shim:${otelAlphaVersion}")
        implementation("io.opentelemetry:opentelemetry-semconv:${otelAlphaVersion}")
        implementation("io.opentelemetry.javaagent:opentelemetry-testing-common:$otelAlphaVersion")
        implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi:$otelVersion")

        implementation("com.twitter:util-core_2.13:$finagleVersion")
        implementation("com.twitter:finagle-core_2.13:$finagleVersion")
        implementation("com.twitter:finagle-http_2.13:$finagleVersion")

        runtimeOnly("ch.qos.logback:logback-classic:1.2.10")
    }

    // Use Scala 2.13 in our library project
    implementation("org.scala-lang:scala-library")

    testImplementation("org.scalatest:scalatest_2.13")
    testImplementation("junit:junit")
    testImplementation("org.scalatestplus:junit-4-13_2.13")
    testRuntimeOnly("com.vladsch.flexmark:flexmark-all:0.35.10")
}

// didn't work;
// used this instead: https://docs.gradle.org/current/samples/sample_building_scala_applications.html
//
//testing {
//    suites {
//        // Configure the built-in test suite
//        val test by getting(JvmTestSuite::class) {
//            // Use JUnit Jupiter test framework
//            useJUnitJupiter("5.8.2")
//        }
//    }
//}
