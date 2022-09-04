package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle
import com.twitter.finagle.http.Request
import com.twitter.finagle.tracing.Tracer
import com.twitter.finagle.{Filter, ServiceFactory, Stack}
import io.dmarkwat.twitter.finagle.tracing.otel.TraceSpanInitializer.TraceSpanParam
import io.opentelemetry.api.trace
import io.opentelemetry.context.propagation.{TextMapPropagator, TextMapSetter}

object HttpClientTraceSpanInitializer {
  val role: Stack.Role = Stack.Role("HttpClientTraceSpanInitializer")

  def apply[Req, Rep](
      otelTracer: trace.Tracer,
      tracer: Tracer,
      propagator: TextMapPropagator
  ): Filter[Req, Rep, Req, Rep] = {
    val setter = new TextMapSetter[Req] {
      override def set(carrier: Req, key: String, value: String): Unit = {
        // oof that's hacky;
        // seems like the only clean way to get around the `typeAgnostic` restrictions?
        carrier.asInstanceOf[Request].headerMap.update(key, value)
      }
    }

    TraceSpanInitializer.client(otelTracer, propagator, setter, tracer)
  }

  def typeAgnostic(
      otelTracer: trace.Tracer,
      tracer: Tracer,
      propagator: TextMapPropagator
  ): Filter.TypeAgnostic =
    new Filter.TypeAgnostic {
      def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = apply(otelTracer, tracer, propagator)
    }

}

/**
 * @see [[com.twitter.finagle.http.HttpClientTraceInitializer]] counterpart
 */
class HttpClientTraceSpanInitializer[Req <: Request, Rep](
    otelTracer: trace.Tracer
) extends Stack.Module2[finagle.param.Tracer, TraceSpanParam, ServiceFactory[Req, Rep]] {
  val role: Stack.Role = HttpClientTraceSpanInitializer.role
  val description: String =
    "Sets the next TraceId and attaches trace information to the outgoing request"
  def make(
      _tracer: finagle.param.Tracer,
      _param: TraceSpanParam,
      next: ServiceFactory[Req, Rep]
  ): ServiceFactory[Req, Rep] = {
    val traceInitializer =
      HttpClientTraceSpanInitializer[Req, Rep](otelTracer, _tracer.tracer, _param.propagator)
    traceInitializer.andThen(next)
  }
}
