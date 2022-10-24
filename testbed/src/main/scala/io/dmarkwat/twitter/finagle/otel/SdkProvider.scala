package io.dmarkwat.twitter.finagle.otel

import io.opentelemetry.api.{GlobalOpenTelemetry, OpenTelemetry}
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider

trait SdkProvider {
  implicit def openTelemetry: OpenTelemetry
}

object SdkProvider {

  trait JavaAgent extends SdkProvider {
    override lazy implicit val openTelemetry: OpenTelemetry = GlobalOpenTelemetry.get()
  }

  trait Library extends SdkProvider {
    // required for testing...but I'm not certain it should be
    override lazy implicit val openTelemetry: OpenTelemetry =
      OpenTelemetrySdk.builder().setTracerProvider(SdkTracerProvider.builder().build()).build()
  }
}
