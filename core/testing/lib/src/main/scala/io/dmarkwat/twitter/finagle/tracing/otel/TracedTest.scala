package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.otel.{SdkProvider, SdkTestCase}
import io.opentelemetry.api.trace.{Span, SpanKind}
import io.opentelemetry.context.Context
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

// conformance test for all different variations of Traced
trait TracedTest {
  self: AnyFlatSpec with should.Matchers with SdkProvider =>

  def traced: Traced

  "The default context" should "be root" in new SdkTestCase {
    traced.context should equal(root)
  }

  it should "cross the framework boundary" in new SdkTestCase {
    Context.current() should equal(root)
  }

  "A span" should "exist in local context" in new SdkTestCase {
    traced.contextOpt should be(empty)

    traced.let(root) {
      traced.contextOpt should not be empty
      traced.hasContext should be(true)
      traced.context should equal(root)

      traced.span should equal(Span.fromContext(root))
    }

    traced.contextOpt should be(empty)
  }

  it should "handle parent conditions" in new SdkTestCase {
    val builder = Traced.spanBuilderFrom(tracer, SpanKind.INTERNAL)

    // explicitly providing root
    traced.letChild(root, builder) {
      traced.context should not equal root
      traced.span should not equal (Span.fromContext(root))
    }

    // with provided context
    traced.letChild(traced.context, builder) {
      traced.context should not equal root
      traced.span should not equal (Span.fromContext(root))
    }

    // without provided context
    traced.letChild(builder) {
      traced.context should not equal root
      traced.span should not equal (Span.fromContext(root))
    }
  }
}
