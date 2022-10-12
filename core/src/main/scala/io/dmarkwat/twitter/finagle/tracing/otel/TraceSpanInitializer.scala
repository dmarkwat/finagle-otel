package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.{Filter, Stack, tracing}
import com.twitter.util.Time
import com.twitter.util.logging.Logging
import io.opentelemetry.api.trace.{Span, SpanKind, Tracer}
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.propagation.{TextMapGetter, TextMapPropagator, TextMapSetter}

import java.util.concurrent.TimeUnit

object TraceSpanInitializer extends Logging {

  def server[Req, Rep](
      otelTracer: Tracer,
      propagator: TextMapPropagator,
      getter: TextMapGetter[Req],
      tracers: tracing.Tracer*
  ): Filter[Req, Rep, Req, Rep] = {
    Filter.mk[Req, Rep, Req, Rep] { (req, svc) =>
      //
      // these are done atomically in this one filter bc otel spans differ from finagle:
      // see https://github.com/open-telemetry/opentelemetry-specification/issues/359
      // in summary: a new span is created whenever a boundary is crossed (client-server, client-client, etc.);
      // whereas finagle expects the span id to be the same between matching client-server interactions
      //

      // extract the parent context from headers, or politely return an invalid context (implicitly via otel)
      val parent = propagator.extract(TraceSpan.context, req, getter)

      TraceSpan.letChild(parent, TraceSpan.spanBuilderFrom(otelTracer, SpanKind.SERVER), tracers: _*) {
        trace("letting: " + TraceSpan.context)
        svc(req) ensure {
          trace("ensuring: " + TraceSpan.context)
          Span.fromContext(TraceSpan.context).end(Time.nowNanoPrecision.inNanoseconds, TimeUnit.NANOSECONDS)
        }
      }
    }
  }

  def client[Req, Rep](
      otelTracer: Tracer,
      propagator: TextMapPropagator,
      setter: TextMapSetter[Req],
      tracers: tracing.Tracer*
  ) =
    Filter.mk[Req, Rep, Req, Rep] { (req, svc) =>
      // make a new child from the current context -- whether that's the unset/root or one provided
      // (e.g. for a client used inside a server context)
      TraceSpan.letChild(TraceSpan.spanBuilderFrom(otelTracer, SpanKind.CLIENT), tracers: _*) {
        propagator.inject(TraceSpan.context, req, setter)
        svc(req) ensure {
          Span.fromContext(TraceSpan.context).end(Time.nowNanoPrecision.inNanoseconds, TimeUnit.NANOSECONDS)
        }
      }
    }

  case class TraceSpanParam(propagator: TextMapPropagator)

  object TraceSpanParam {
    implicit val param: Stack.Param[TraceSpanParam] =
      Stack.Param(TraceSpanParam(GlobalOpenTelemetry.getPropagators.getTextMapPropagator))
  }
}
