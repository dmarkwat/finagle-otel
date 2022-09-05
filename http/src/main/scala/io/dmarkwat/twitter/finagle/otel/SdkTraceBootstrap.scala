package io.dmarkwat.twitter.finagle.otel

import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.{SdkTracerProvider, SpanLimits, SpanProcessor}

import scala.jdk.CollectionConverters.IterableHasAsJava

trait SdkTraceBootstrap {
  self: ResourceIdentity =>

  lazy val traceSampler: Sampler = Sampler.alwaysOn()
  lazy val traceSpanLimits: SpanLimits = SpanLimits.getDefault

  lazy val traceSpanProcessors: List[SpanProcessor] = List.empty

  lazy val traceProvider: SdkTracerProvider =
    SdkTracerProvider
      .builder()
      .setResource(self.resource)
      .setSampler(traceSampler)
      .setSpanLimits(traceSpanLimits)
      .addSpanProcessor(SpanProcessor.composite(traceSpanProcessors.asJava))
      // setClock could be wired for finagle's Time, but experience suggests this global otel change might be unwise
      .build()
}
