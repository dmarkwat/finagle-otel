package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.otel.{SdkProvider, SdkTestCase}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class ContextStorageTest extends AnyFlatSpec with should.Matchers with SdkProvider.Library {

  "ContextStorage" should "get current context" in new SdkTestCase {
    val storage = new ContextStorage

    storage.current() should be(null)

    ContextStorage.containedOver(root) {
      storage.current() should be(root)
    }

    val ctx = randomContext
    ContextStorage.containedOver(ctx) {
      storage.current() should be(ctx)
    }
  }

  "ContextStorage" should "attach context" in new SdkTestCase {
    val storage = new ContextStorage

    an[Exception] should be thrownBy {
      storage.attach(root)
    }

    ContextStorage.ensure {
      // the otel ContextStorage API specifically uses null when nothing is set
      storage.current() should be(null)
    }

    ContextStorage.ensure {
      storage.current() should be(null)

      val scope = storage.attach(root)
      scope should not be ContextStorage.NoopScope

      storage.current() should be(root)
      scope.close()

      storage.current() should be(null)
    }
  }
}
