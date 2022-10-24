package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.tracing.Tracer
import com.twitter.util.Time
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace
import io.opentelemetry.api.trace.{Span, SpanKind}
import io.opentelemetry.context.Context

import java.util.concurrent.TimeUnit

trait Traced {
  // explicitly let the context as current
  def let[O](context: Context, tracers: Tracer*)(f: => O): O

  def contextOpt: Option[Context]

  // make a child span from the given parent context
  def letChild[O](parent: Context, span: Context => Span, tracers: Tracer*)(f: => O): O = {
    val context = Context
      .root()
      .`with`(span(parent))
      .`with`(Baggage.empty())

    this.let(context, tracers: _*)(f)
  }

  // make a child span with the current context as parent
  def letChild[O](span: Context => Span, tracers: Tracer*)(f: => O): O = letChild(this.context, span, tracers: _*)(f)

  // for debugging and short-circuiting
  def hasContext: Boolean = this.contextOpt.isDefined

  // the current context from finagle's POV
  def context: Context = contextOpt.getOrElse(Context.root())

  def span: Span = Span.fromContext(context)
}

object Traced {

  private[otel] val contextKey = Contexts.local.newKey[Traced]()

  val default: Traced = CompositeTraced

  def let[R](t: Traced)(f: => R): R = {
    Contexts.local.let(contextKey, t)(f)
  }

  def get: Traced = Contexts.local.get(contextKey).getOrElse(default)

  def spanBuilderFrom(
      tracer: trace.Tracer,
      kind: SpanKind,
      startTime: Time = Time.nowNanoPrecision
  ): Context => Span = { parent =>
    tracer
      // null or empty string will use the otel default
      .spanBuilder(null)
      .setParent(parent)
      .setSpanKind(kind)
      .setStartTimestamp(startTime.inNanoseconds, TimeUnit.NANOSECONDS)
      .startSpan()
  }
}
