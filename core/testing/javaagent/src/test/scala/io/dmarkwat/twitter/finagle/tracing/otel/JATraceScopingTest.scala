package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.otel.SdkProvider
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JATraceScopingTest extends TraceScopingTest with EnsureJavaAgent with SdkProvider.JavaAgent {}
