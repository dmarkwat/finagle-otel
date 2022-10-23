package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.context.Contexts.local
import com.twitter.finagle.tracing.{Trace, Tracer}
import com.twitter.util.Time
import io.dmarkwat.twitter.finagle.tracing.otel.TraceSpan.TraceSpanEventing.EventTracing
import io.opentelemetry.api.common.{AttributeKey, Attributes, AttributesBuilder}
import io.opentelemetry.context.Context
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_EVENT_NAME

import java.util.concurrent.TimeUnit

object TraceSpan extends Traced {

  // opting not to broadcast at this time as in the current impl it's redundant
  private[otel] val contextKey = Contexts.local.newKey[Context]()

  // explicitly let the context as current
  override def let[O](context: Context, tracers: Tracer*)(f: => O): O = {
    Contexts.local.let(contextKey, context) {
      Trace.letTracers(tracers) {
        TraceSpanEventing.let(f)
      }
    }
  }

  override def contextOpt: Option[Context] = Contexts.local.get(contextKey)

  def eventing(t: EventTracing => EventTracing): Unit = TraceSpanEventing.get.foreach(_.updated(t))

  private[otel] object TraceSpanEventing {
    val eventKey: local.Key[Container] = Contexts.local.newKey[Container]()

    def get: Option[Container] = Contexts.local.get(eventKey)

    def let[O](f: => O): O = Contexts.local.let(eventKey, new Container())(f)

    final class Container(private var tracing: EventTracing = NotTracing) {
      def updated(t: EventTracing => EventTracing): Unit = tracing = t(tracing)
    }

    sealed trait EventTracing {
      def withAttribute[T](key: AttributeKey[T], value: T): EventTracing

      def withAttributes(attributes: Attributes): EventTracing

      def withName(name: String): EventTracing

      def newEvent(name: String): EventTracing

      // starts error recording (see ClientExceptionTracingFilter for one example)
      def newException(): EventTracing

      def record(timestamp: Time): EventTracing
    }

    case object NotTracing extends EventTracing {
      override def withAttribute[T](key: AttributeKey[T], value: T): EventTracing =
        ActivelyTracing(attrs = Attributes.builder().put(key, value))

      override def withAttributes(attributes: Attributes): EventTracing =
        ActivelyTracing(attrs = Attributes.builder().putAll(attributes))

      // hopefully never called -- withName is an update method, not a "new event" method
      override def withName(name: String): EventTracing = ActivelyTracing(name = name)

      override def newEvent(name: String): EventTracing = ActivelyTracing(name = name)

      override def newException(): EventTracing = ActivelyTracing(name = EXCEPTION_EVENT_NAME)

      // hopefully never called -- this will record a dummy event
      override def record(timestamp: Time): EventTracing = ActivelyTracing().record(timestamp)
    }

    object ActivelyTracing {
      def apply(name: String): ActivelyTracing = ActivelyTracing(name = Some(name))
    }

    case class ActivelyTracing(
        name: Option[String] = None,
        at: Time = Time.nowNanoPrecision,
        attrs: AttributesBuilder = Attributes.builder()
    ) extends EventTracing {
      override def withAttribute[T](key: AttributeKey[T], value: T): EventTracing = {
        attrs.put(key, value)
        this
      }

      override def withAttributes(attributes: Attributes): EventTracing = {
        attrs.putAll(attributes)
        this
      }

      override def withName(name: String): EventTracing = this.copy(name = Some(name))

      override def newEvent(name: String): EventTracing = {
        // todo should record current event???
        NotTracing.newEvent(name)
      }

      override def newException(): EventTracing = {
        // todo should record current event???
        NotTracing.newException()
      }

      override def record(timestamp: Time = at): EventTracing = {
        TraceSpan.span.addEvent(name.getOrElse("unknown"), attrs.build(), timestamp.inNanoseconds, TimeUnit.NANOSECONDS)
        NotTracing
      }
    }
  }
}
