package io.dmarkwat.twitter.finagle.tracing.otel.testing

import io.dmarkwat.twitter.finagle.otel.SdkProvider
import io.dmarkwat.twitter.finagle.tracing.otel.CompositeTracedTest

class JACompositeTracedTest extends CompositeTracedTest with EnsureJavaAgent with SdkProvider.JavaAgent {}
