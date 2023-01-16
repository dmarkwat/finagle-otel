plugins {
    id("io.dmarkwat.twitter.finagle.scala-library-conventions")
    id("java-test-fixtures")
}

val scalified: (String) -> String by extra

dependencies {
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-semconv")

    implementation("org.slf4j:jul-to-slf4j:1.7.32")

    implementation(scalified("com.twitter:util-core"))
    implementation(scalified("com.twitter:finagle-core"))

    api(project(":finagle-bridge"))
    testImplementation(project(":testbed"))
}
