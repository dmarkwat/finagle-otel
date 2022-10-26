package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.otel.{SdkProvider, SdkTestCase}
import io.dmarkwat.twitter.finagle.tracing.otel.Implicits.RichContext
import io.opentelemetry.context.Context
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.util.Using

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CompositeTracedTest extends AnyFlatSpec with should.Matchers with SdkProvider.Library with TracedTest {
  override def traced: Traced = CompositeTraced

  it should "externalize across the framework boundary" in new SdkTestCase {
    traced.let(root) {
      Context.current should equal(root)
    }

    val ctx = randomContext
    traced.let(ctx) {
      Context.current.asSpan should equal(ctx.asSpan)
    }
    Context.current should equal(root)

    ContextStorage.ensure {
      // the reverse should not be true because:
      // - assignments to Context.current outside the finagle stack are treated as necessarily out of band
      // - all interaction with that OOB control flow must be rooted at the point of its origin (the ContextContainer it was set in)
      // - any return to the finagle stack would be in a different, independently-configured Service call chain which would need independent instrumentation
      Using.resource(randomContext.makeCurrent()) { _ =>
        traced.context should not equal ctx
      }
    }
  }
}
