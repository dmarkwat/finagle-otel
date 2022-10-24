package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle._
import com.twitter.util.Future
import io.dmarkwat.twitter.finagle.tracing.otel.param

// add just after any filters that create the requisite context, found in TraceSpan
class TracedInitializer[Req, Rep](traced: Traced) extends SimpleFilter[Req, Rep] {
  override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    TraceScoping.extern.wrapping(traced.context, service)(request)
  }
}

object TracedInitializer {
  val role: Stack.Role = Stack.Role("TracedInitializer")

  def module[Req, Rep]: Stackable[ServiceFactory[Req, Rep]] =
    new Stack.Module1[param.Traced, ServiceFactory[Req, Rep]] {
      val role: Stack.Role = TracedInitializer.role
      val description = "Initializes the Traced instance for use in the Service stack"

      def make(
          traced: param.Traced,
          next: ServiceFactory[Req, Rep]
      ): ServiceFactory[Req, Rep] = {
        new TracedInitializer[Req, Rep](traced.traced).andThen(next)
      }
    }
}
