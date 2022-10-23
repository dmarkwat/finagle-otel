package io.dmarkwat.twitter.finagle.tracing.otel.testing

import io.dmarkwat.twitter.finagle.otel.SdkProvider
import io.dmarkwat.twitter.finagle.tracing.otel.TraceScopingTest

class JATraceScopingTest extends TraceScopingTest with SdkProvider.JavaAgent {}
