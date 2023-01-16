plugins {
    id("io.dmarkwat.twitter.finagle.scala-library-conventions")
    `java-test-fixtures`
}

val otelVersion: String by extra
val otelAlphaVersion: String by extra

dependencies {
    compileOnly("io.opentelemetry:opentelemetry-sdk:$otelVersion")
    compileOnly("io.opentelemetry.javaagent:opentelemetry-testing-common:$otelAlphaVersion")
    api("org.scalatest:scalatest_2.13:3.2.12")
    api("com.twitter:finagle-core_2.13:22.7.0")

    implementation("org.scalatest:scalatest_2.13")
    implementation("junit:junit")
    implementation("org.scalatestplus:junit-4-13_2.13")
}
