package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle
import com.twitter.finagle._
import com.twitter.finagle.http.Request
import io.dmarkwat.twitter.finagle.tracing.otel.TraceSpanInitializer.TraceSpanParam
import io.opentelemetry.api.trace
import io.opentelemetry.context.propagation.TextMapGetter

import java.lang
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsJava}

object HttpServerTraceSpanInitializer {
  val role: Stack.Role = Stack.Role("HttpServerTraceSpanInitializer")
}

/**
 * @see [[com.twitter.finagle.http.HttpServerTraceInitializer]] counterpart
 */
class HttpServerTraceSpanInitializer[Req <: Request, Rep](
    otelTracer: trace.Tracer
) extends Stack.Module2[finagle.param.Tracer, TraceSpanParam, ServiceFactory[Req, Rep]] {
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

    TraceSpanInitializer.server(otelTracer, propagator, getter, tracer).andThen(next)
  }
}
