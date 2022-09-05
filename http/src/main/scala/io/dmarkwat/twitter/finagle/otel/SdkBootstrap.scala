package io.dmarkwat.twitter.finagle.otel

import io.dmarkwat.twitter.finagle.otel.SdkBootstrap.InstrumentationScopeName
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.OpenTelemetrySdk

trait SdkBootstrap {
  self: SdkTraceBootstrap =>

  lazy val otelSdk: OpenTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(self.traceProvider).build()

  lazy val otelTracer: Tracer = self.traceProvider.get(InstrumentationScopeName)
}

object SdkBootstrap {
  val InstrumentationScopeName = "finagle-otel"

  def tracer: Tracer = GlobalOpenTelemetry.getTracer(InstrumentationScopeName)

  /**
   * Mix in to makes the [[SdkBootstrap.otelSdk]] the global one.
   */
  trait Globalized {
    self: SdkBootstrap =>

    GlobalOpenTelemetry.set(otelSdk)
  }

  trait Auto {
    lazy val otelTracer: Tracer = tracer
  }
}
