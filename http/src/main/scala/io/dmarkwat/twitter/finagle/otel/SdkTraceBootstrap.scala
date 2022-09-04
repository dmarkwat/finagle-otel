package io.dmarkwat.twitter.finagle.otel

import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.{SdkTracerProvider, SpanLimits}

trait SdkTraceBootstrap {
  self: ResourceIdentity =>

  val traceSampler: Sampler = Sampler.alwaysOn()
  val traceSpanLimits: SpanLimits = SpanLimits.getDefault

  // todo could add span processor(s) to aid in debugging, logging, etc.

  val traceProvider: SdkTracerProvider =
    SdkTracerProvider
      .builder()
      .setResource(self.resource)
      .setSampler(traceSampler)
      .setSpanLimits(traceSpanLimits)
      // setClock could be wired for finagle's Time, but experience suggests this global otel change might be unwise
      .build()
}
