package io.dmarkwat.twitter.finagle.otel

import io.opentelemetry.api.{GlobalOpenTelemetry, OpenTelemetry}
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider

trait SdkProvider {
  implicit def otel: OpenTelemetry
}

object SdkProvider {

  trait JavaAgent extends SdkProvider {
    override lazy implicit val otel: OpenTelemetry = GlobalOpenTelemetry.get()
  }

  trait Library extends SdkProvider {
    // required for testing...but I'm not certain it should be
    override lazy implicit val otel: OpenTelemetry =
      OpenTelemetrySdk.builder().setTracerProvider(SdkTracerProvider.builder().build()).build()
  }
}
