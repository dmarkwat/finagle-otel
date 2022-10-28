plugins {
    id("io.dmarkwat.twitter.finagle.scala-library-conventions")
    id("java-test-fixtures")
}

dependencies {
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-semconv")

    implementation("org.slf4j:jul-to-slf4j:1.7.32")

    implementation("com.twitter:util-core_2.13")
    implementation("com.twitter:finagle-core_2.13")

    api(project(":finagle-bridge"))
    testImplementation(project(":testbed"))
}
