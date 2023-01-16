plugins {
    id("io.dmarkwat.twitter.finagle.scala-library-conventions")
}

dependencies {
    implementation("io.opentelemetry:opentelemetry-sdk")

    implementation("org.scalatest:scalatest_2.13")
    implementation("junit:junit")
    implementation("org.scalatestplus:junit-4-13_2.13")

    implementation(project(":testbed"))
    implementation(project(":core"))

    runtimeOnly("ch.qos.logback:logback-classic")
}
