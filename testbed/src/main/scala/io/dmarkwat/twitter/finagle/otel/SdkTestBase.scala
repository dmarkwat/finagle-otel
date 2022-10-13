package io.dmarkwat.twitter.finagle.otel

import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider

trait SdkTestBase {
  // required for testing...but I'm not certain it should be
  val otel: OpenTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(SdkTracerProvider.builder().build()).build()
  val tracer: Tracer = otel.getTracerProvider.get("test")
}
