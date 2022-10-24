package io.dmarkwat.twitter.finagle.tracing.otel.param

import com.twitter.finagle.Stack
import io.dmarkwat.twitter.finagle.tracing.otel

case class Traced(traced: otel.Traced) {
  def mk(): (Traced, Stack.Param[Traced]) =
    (this, Traced.param)
}

object Traced {
  implicit val param: Stack.Param[Traced] = Stack.Param(Traced(otel.Traced.default))
}
