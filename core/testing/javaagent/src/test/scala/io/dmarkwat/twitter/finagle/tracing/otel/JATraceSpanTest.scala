package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.otel.SdkProvider
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JATraceSpanTest extends TraceSpanTest with EnsureJavaAgent with SdkProvider.JavaAgent {}
