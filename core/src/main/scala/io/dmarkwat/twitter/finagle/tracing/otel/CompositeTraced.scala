package io.dmarkwat.twitter.finagle.tracing.otel
import com.twitter.finagle.tracing.Tracer
import io.opentelemetry.context.Context

object CompositeTraced extends Traced {
  override def let[O](context: Context, tracers: Tracer*)(f: => O): O =
    TraceSpan.let(context, tracers: _*) {
      ContextStorage.containedOver(context) {
        f
      }
    }

  override def contextOpt: Option[Context] = TraceSpan.contextOpt
}
