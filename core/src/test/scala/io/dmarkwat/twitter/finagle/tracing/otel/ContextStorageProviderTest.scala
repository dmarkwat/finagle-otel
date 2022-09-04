package io.dmarkwat.twitter.finagle.tracing.otel

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class ContextStorageProviderTest
    extends AnyFlatSpec
    with should.Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll {}
