package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.tracing.otel
import io.dmarkwat.twitter.finagle.otel.SdkProvider
import io.opentelemetry.context.ContextStorage
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

trait EnsureJavaAgent {
  self: AnyFlatSpec with should.Matchers with SdkProvider =>

  "The javaagent" should "be loaded" in {
    ContextStorage.get() should not be a[otel.ContextStorage]
  }
}
