package io.dmarkwat.twitter.finagle.otel

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.{SpanKind, Tracer}
import io.opentelemetry.context.{Context, ContextStorage, ContextStorageProvider}

abstract class SdkTestCase()(implicit openTelemetry: => OpenTelemetry, storageProvider: => ContextStorageProvider) {
  lazy val tracer: Tracer = openTelemetry.getTracerProvider.get("test")

  lazy val storage: ContextStorage = storageProvider.get()

  def root: Context = storage.root()

  def current(): Context = storage.current()

  def randomContext: Context = storage
    .root()
    .`with`(
      tracer
        // null or empty string will use the otel default
        .spanBuilder(null)
        .setSpanKind(SpanKind.INTERNAL)
        .startSpan()
    )
}
