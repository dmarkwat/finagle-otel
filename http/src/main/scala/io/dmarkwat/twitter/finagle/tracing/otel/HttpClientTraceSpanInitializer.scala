package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle
import com.twitter.finagle.http.Request
import com.twitter.finagle.tracing.Tracer
import com.twitter.finagle.{Filter, ServiceFactory, Stack}
import com.twitter.util.Time
import io.dmarkwat.twitter.finagle.tracing.otel.TraceSpanInitializer.TraceSpanParam
import io.opentelemetry.api.trace
import io.opentelemetry.api.trace.{Span, SpanKind}
import io.opentelemetry.context.propagation.{TextMapPropagator, TextMapSetter}

import java.util.concurrent.TimeUnit

object HttpClientTraceSpanInitializer {
  val role: Stack.Role = Stack.Role("HttpClientTraceSpanInitializer")

  def apply[Req, Rep](
      otelTracer: trace.Tracer,
      tracer: Tracer,
      spanKind: SpanKind,
      propagator: TextMapPropagator
  ): Filter[Req, Rep, Req, Rep] = {
    val setter = new TextMapSetter[Request] {
      override def set(carrier: Request, key: String, value: String): Unit = carrier.headerMap.update(key, value)
    }

    Filter.mk[Req, Rep, Req, Rep] { (req, svc) =>
      // make a new child from the current context -- whether that's the unset/root or one provided
      // (e.g. for a client used inside a server context)
      TraceSpan.letChild(TraceSpan.spanBuilderFrom(otelTracer, spanKind), tracer) {
        propagator.inject(TraceSpan.context, req.asInstanceOf[Request], setter)
        svc(req) ensure {
          Span.fromContext(TraceSpan.context).end(Time.nowNanoPrecision.inNanoseconds, TimeUnit.NANOSECONDS)
        }
      }
    }
  }

  def typeAgnostic(
      otelTracer: trace.Tracer,
      tracer: Tracer,
      spanKind: SpanKind,
      propagator: TextMapPropagator
  ): Filter.TypeAgnostic =
    new Filter.TypeAgnostic {
      def toFilter[Req, Rep]: Filter[Req, Rep, Req, Rep] = apply(otelTracer, tracer, spanKind, propagator)
    }

}

/**
 * @see [[com.twitter.finagle.http.HttpClientTraceInitializer]] counterpart
 */
class HttpClientTraceSpanInitializer[Req <: Request, Rep](
    otelTracer: trace.Tracer,
    spanKind: SpanKind = SpanKind.CLIENT
) extends Stack.Module2[finagle.param.Tracer, TraceSpanParam, ServiceFactory[Req, Rep]] {
  require(spanKind != SpanKind.SERVER)

  val role: Stack.Role = HttpClientTraceSpanInitializer.role
  val description: String =
    "Sets the next TraceId and attaches trace information to the outgoing request"
  def make(
      _tracer: finagle.param.Tracer,
      _param: TraceSpanParam,
      next: ServiceFactory[Req, Rep]
  ): ServiceFactory[Req, Rep] = {
    val traceInitializer =
      HttpClientTraceSpanInitializer[Req, Rep](otelTracer, _tracer.tracer, spanKind, _param.propagator)
    traceInitializer.andThen(next)
  }
}
