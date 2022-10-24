package io.dmarkwat.twitter.finagle.otel

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.{SpanKind, Tracer}
import io.opentelemetry.context.Context

abstract class SdkTestCase()(implicit openTelemetry: => OpenTelemetry) {
  lazy val tracer: Tracer = openTelemetry.getTracerProvider.get("test")

  def root: Context = Context.root()

  def randomContext: Context = Context
    .root()
    .`with`(
      tracer
        // null or empty string will use the otel default
        .spanBuilder(null)
        .setSpanKind(SpanKind.INTERNAL)
        .startSpan()
    )
}

object SdkTestCase {

  def in(fn: SdkTestCase => Unit)(implicit otel: => OpenTelemetry): Unit = fn(new SdkTestCase()(otel) {})
}
