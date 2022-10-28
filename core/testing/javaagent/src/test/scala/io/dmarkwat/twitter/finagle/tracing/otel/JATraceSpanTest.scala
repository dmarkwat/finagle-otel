package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.otel.SdkProvider

class JATraceSpanTest extends TraceSpanTest with EnsureJavaAgent with SdkProvider.JavaAgent {}
