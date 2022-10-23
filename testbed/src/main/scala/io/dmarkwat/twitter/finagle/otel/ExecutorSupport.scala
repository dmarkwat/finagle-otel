package io.dmarkwat.twitter.finagle.otel

import org.scalatest.BeforeAndAfterEach

import java.util.concurrent.ExecutorService

trait ExecutorSupport {
  self: BeforeAndAfterEach =>

  def executor: ExecutorService

  override protected def beforeEach(): Unit = {}

  override protected def afterEach(): Unit = {
    executor.shutdownNow()
  }
}
