package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.Stack
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.TextMapPropagator

object TraceSpanInitializer {
  case class TraceSpanParam(propagator: TextMapPropagator)

  object TraceSpanParam {
    implicit val param: Stack.Param[TraceSpanParam] =
      Stack.Param(TraceSpanParam(W3CTraceContextPropagator.getInstance()))
  }
}
