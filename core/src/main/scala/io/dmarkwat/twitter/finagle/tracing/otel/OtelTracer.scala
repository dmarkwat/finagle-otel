package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.context.Contexts.local
import com.twitter.finagle.tracing.Annotation.{BinaryAnnotation, Rpc, ServiceName}
import com.twitter.finagle.tracing.{Annotation, Record, TraceId, Tracer}
import com.twitter.util.logging.Logging
import com.twitter.util.{Duration, Time}
import io.dmarkwat.twitter.finagle.tracing.otel.OtelTracer.TraceMeta
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes._

/**
 * Wires up the finagle [[Tracer]] interface for http tracing backed by Otel entirely on finagle [[com.twitter.finagle.context.Contexts]].
 *
 * Top-level [[Tracer]] for handling the most common cases in the finagle stack.
 */
abstract class OtelTracer extends Tracer {
  self: Logging =>

  /**
   * [[AttributeKey]] for mapping [[ServiceName]] down to its otel tag counterpart.
   */
  val serviceNameAttr: AttributeKey[String]

  /**
   * A stacked set of [[PartialFunction]]s for handling [[Annotation]]s.
   *
   * Obtain precise timestamp and duration values for when the annotation was generated
   * via [[OtelTracer.TraceMeta.get]].
   *
   * @return a [[PartialFunction]]; the default case need not be handled as it's
   *         handled by [[OtelTracer.record()]]
   */
  def annotationHandlers: PartialFunction[Annotation, Unit] = {
    case Rpc(name) =>
      TraceSpan.span.updateName(name)
    // otel attributes: peer.service and/or rpc.service
    //  peer.service => client attribute
    //  rpc service => server attribute
    // handled by impls
    case ServiceName(service) =>
      TraceSpan.span.setAttribute(serviceNameAttr, service)

    // see ClientExceptionTracingFilter
    // starts the event recording machinery: finagle's in-tree exception Recording is a multi-Record process
    case BinaryAnnotation("error", true) =>
      TraceSpan.eventing(_.newException())
      // todo find a way to determine which errors are terminal so we can set the span status correctly?
      //  however, presumably, if the span eventually succeeds it will yield an http status which will overwrite the status...
      TraceSpan.span.setStatus(StatusCode.ERROR)
    case BinaryAnnotation("exception.type", typeName: String) =>
      TraceSpan.eventing(_.withAttribute(EXCEPTION_TYPE, typeName))
    case BinaryAnnotation("exception.message", message: String) =>
      TraceSpan.eventing(_.withAttribute(EXCEPTION_MESSAGE, message).record(TraceMeta.get.timestamp))
  }

  override def record(record: Record): Unit = {
    TraceMeta.let(TraceMeta(record.timestamp, record.duration)) {
      annotationHandlers
        .orElse[Annotation, Unit] {
          // catch all
          case a => debug("Annotation unhandled: " + a)
        }
        .apply(record.annotation)
    }
  }

  // always sample when working with otel -- it will make the decisions
  override def sampleTrace(traceId: TraceId): Option[Boolean] = Tracer.SomeTrue
}

object OtelTracer {
  case class TraceMeta(timestamp: Time, duration: Option[Duration])

  object TraceMeta {
    private val key: local.Key[TraceMeta] = Contexts.local.newKey()

    def get: TraceMeta = Contexts.local.get(key).get

    def let(meta: TraceMeta)(fn: => Unit): Unit = Contexts.local.let(key, meta)(fn)
  }
}
