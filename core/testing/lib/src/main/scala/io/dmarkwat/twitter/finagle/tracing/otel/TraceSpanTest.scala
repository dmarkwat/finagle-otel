package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.otel.SdkProvider
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TraceSpanTest extends AnyFlatSpec with should.Matchers with SdkProvider.Library with TracedTest {
  override def traced: Traced = TraceSpan
}
