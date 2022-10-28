package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.otel.SdkProvider

class JATraceScopingTest extends TraceScopingTest with EnsureJavaAgent with SdkProvider.JavaAgent {}
