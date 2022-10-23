package io.dmarkwat.twitter.finagle.otel

import io.opentelemetry.api.GlobalOpenTelemetry
import org.scalatest.{BeforeAndAfterEach, Suite}

trait ResettingTelemetry extends BeforeAndAfterEach {
  self: Suite =>

  abstract override protected def beforeEach(): Unit = {
    super.beforeEach()
    GlobalOpenTelemetry.resetForTest()
  }
}
