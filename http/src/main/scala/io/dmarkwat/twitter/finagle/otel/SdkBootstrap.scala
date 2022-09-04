package io.dmarkwat.twitter.finagle.otel

import io.dmarkwat.twitter.finagle.otel.SdkBootstrap.InstrumentationScopeName
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.OpenTelemetrySdk

trait SdkBootstrap {
  self: SdkTraceBootstrap =>

  OpenTelemetrySdk.builder().setTracerProvider(self.traceProvider).buildAndRegisterGlobal()

  def otelTracer: Tracer = self.traceProvider.get(InstrumentationScopeName)
}

object SdkBootstrap {
  val InstrumentationScopeName = "finagle-otel"

  def tracer: Tracer = GlobalOpenTelemetry.getTracer(InstrumentationScopeName)
}
