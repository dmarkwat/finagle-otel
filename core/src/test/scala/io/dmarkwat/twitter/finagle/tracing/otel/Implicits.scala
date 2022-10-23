package io.dmarkwat.twitter.finagle.tracing.otel

import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context

object Implicits {

  implicit class RichContext(context: Context) {
    def asSpan: Span = Span.fromContext(context)
  }
}
