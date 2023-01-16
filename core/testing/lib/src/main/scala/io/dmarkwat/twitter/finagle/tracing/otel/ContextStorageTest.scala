package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.context.Contexts
import io.dmarkwat.twitter.finagle.BaseTestSpec
import io.dmarkwat.twitter.finagle.otel.{SdkProvider, SdkTestCase}
import io.dmarkwat.twitter.finagle.tracing.otel.ContextStorage.{ContextContainer, ctxKey}
import io.opentelemetry.context.Context
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

import scala.util.Using

@RunWith(classOf[JUnitRunner])
class ContextStorageTest extends BaseTestSpec with SdkProvider.Library with ExplicitStorage {

  def containedOver[O](context: Context)(f: => O): O = {
    Contexts.local.let(ctxKey, new ContextContainer(context)) {
      f
    }
  }

  "ContextStorage" should "get current context" in new SdkTestCase {
    storage.current() should be(null)

    containedOver(root) {
      storage.current() should be(root)
    }

    val ctx = randomContext
    containedOver(ctx) {
      storage.current() should be(ctx)
    }
  }

  "ContextStorage" should "attach context" in new SdkTestCase {
    an[Exception] should be thrownBy {
      storage.attach(root)
    }

    ContextStorage.ensure {
      // the otel ContextStorage API specifically uses null when nothing is set
      storage.current() should be(null)
    }

    ContextStorage.ensure {
      storage.current() should be(null)

      Using.resource(storage.attach(root)) { scope =>
        scope should not be ContextStorage.NoopScope

        storage.current() should be(root)
      }

      storage.current() should be(null)
    }

    ContextStorage.ensure {
      storage.current() should be(null)

      Using.resource(storage.attach(root)) { scope =>
        scope should not be ContextStorage.NoopScope

        storage.current() should be(root)

        Using.resource(storage.attach(root)) { scope =>
          scope should be(ContextStorage.NoopScope)

          storage.current() should be(root)
        }

        storage.current() should be(root)

        val ctx = randomContext
        Using.resource(storage.attach(ctx)) { scope =>
          scope should not be ContextStorage.NoopScope

          storage.current() should be(ctx)
        }

        storage.current() should be(root)
      }

      storage.current() should be(null)
    }
  }
}
