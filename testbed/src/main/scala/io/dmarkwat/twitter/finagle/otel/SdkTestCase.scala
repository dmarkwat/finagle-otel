package io.dmarkwat.twitter.finagle.otel

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.{SpanKind, Tracer}
import io.opentelemetry.context.{Context, ContextStorage, ContextStorageProvider}

abstract class SdkTestCase()(implicit openTelemetry: => OpenTelemetry) {
  lazy val tracer: Tracer = openTelemetry.getTracerProvider.get("test")

  def root: Context = Context.root()

  def current(): Context = Context.current()

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
