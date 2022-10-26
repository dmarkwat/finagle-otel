plugins {
    id("io.dmarkwat.twitter.finagle.scala-library-conventions")
}

dependencies {
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-semconv")
    implementation("com.twitter:finagle-http_2.13")

    testImplementation(project(":testbed"))

    api(project(":core"))
}
