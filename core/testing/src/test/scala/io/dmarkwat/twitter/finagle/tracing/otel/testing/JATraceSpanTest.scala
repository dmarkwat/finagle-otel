package io.dmarkwat.twitter.finagle.tracing.otel.testing

import io.dmarkwat.twitter.finagle.otel.SdkProvider
import io.dmarkwat.twitter.finagle.tracing.otel.TraceSpanTest

class JATraceSpanTest extends TraceSpanTest with SdkProvider.JavaAgent {}
