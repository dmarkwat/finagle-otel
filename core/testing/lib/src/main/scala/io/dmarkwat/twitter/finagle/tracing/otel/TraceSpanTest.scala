package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.BaseTestSpec
import io.dmarkwat.twitter.finagle.otel.SdkProvider
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TraceSpanTest extends BaseTestSpec with SdkProvider.Library with TracedTest {
  override def traced: Traced = TraceSpan
}
