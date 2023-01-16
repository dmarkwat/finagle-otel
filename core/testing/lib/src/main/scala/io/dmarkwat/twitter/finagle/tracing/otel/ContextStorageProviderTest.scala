package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.BaseTestSpec
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ContextStorageProviderTest extends BaseTestSpec with BeforeAndAfterEach with BeforeAndAfterAll {}
