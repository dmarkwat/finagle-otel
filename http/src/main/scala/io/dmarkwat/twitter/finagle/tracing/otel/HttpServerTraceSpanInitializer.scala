package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle
import com.twitter.finagle._
import com.twitter.finagle.http.Request
import com.twitter.util.Time
import io.dmarkwat.twitter.finagle.tracing.otel.TraceSpanInitializer.TraceSpanParam
import io.opentelemetry.api.trace
import io.opentelemetry.api.trace.{Span, SpanKind}
import io.opentelemetry.context.propagation.TextMapGetter

import java.lang
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsJava}

object HttpServerTraceSpanInitializer {
  val role: Stack.Role = Stack.Role("HttpServerTraceSpanInitializer")
}

/**
 * @see [[com.twitter.finagle.http.HttpServerTraceInitializer]] counterpart
 */
class HttpServerTraceSpanInitializer[Req <: Request, Rep](
    otelTracer: trace.Tracer,
    spanKind: SpanKind = SpanKind.SERVER
) extends Stack.Module2[finagle.param.Tracer, TraceSpanParam, ServiceFactory[Req, Rep]] {
  require(spanKind != SpanKind.CLIENT)

  val role: Stack.Role = HttpServerTraceSpanInitializer.role
  val description: String =
    "Initialize the tracing system with trace info from the incoming request"

  def make(
      _tracer: finagle.param.Tracer,
      _param: TraceSpanParam,
      next: ServiceFactory[Req, Rep]
  ): ServiceFactory[Req, Rep] = {
    val finagle.param.Tracer(tracer) = _tracer
    val TraceSpanParam(propagator) = _param

    val getter = new TextMapGetter[Req] {
      override def keys(carrier: Req): lang.Iterable[String] = {
        // presumably faster than creating sets and performing intersection
        propagator.fields().asScala.filter(carrier.headerMap.contains(_)).asJava
      }

      override def get(carrier: Req, key: String): String = carrier.headerMap.getOrNull(key)
    }

    val traceInitializer = Filter.mk[Req, Rep, Req, Rep] { (req, svc) =>
      //
      // these are done atomically in this one filter bc otel spans differ from finagle:
      // see https://github.com/open-telemetry/opentelemetry-specification/issues/359
      // in summary: a new span is created whenever a boundary is crossed (client-server, client-client, etc.);
      // whereas finagle expects the span id to be the same between matching client-server interactions
      //

      // extract the parent context from headers, or politely return an invalid context (implicitly via otel)
      val parent = propagator.extract(TraceSpan.context, req, getter)

      TraceSpan.letChild(parent, TraceSpan.spanBuilderFrom(otelTracer, spanKind), tracer) {
        svc(req) ensure {
          Span.fromContext(TraceSpan.context).end(Time.nowNanoPrecision.inNanoseconds, TimeUnit.NANOSECONDS)
        }
      }
    }
    traceInitializer.andThen(next)
  }
}
