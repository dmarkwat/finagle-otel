plugins {
    id("io.dmarkwat.twitter.finagle.scala-library-conventions")
}

dependencies {
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-opencensus-shim")

    implementation(project(":http"))

    implementation("com.twitter:finagle-http_2.13")
    implementation("com.google.cloud:google-cloud-spanner:6.29.1")
    implementation("io.grpc:grpc-okhttp:1.48.0")
    testImplementation("com.typesafe.play:play-json_2.13:2.8.2")
    testImplementation("org.scalatestplus:mockito-4-5_2.13:3.2.12.0")
}
