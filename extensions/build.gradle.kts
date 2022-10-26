plugins {
    id("io.dmarkwat.twitter.finagle.scala-library-conventions")
}

dependencies {
    implementation("io.opentelemetry:opentelemetry-semconv")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")

    implementation(project(":finagle-bridge"))
}
