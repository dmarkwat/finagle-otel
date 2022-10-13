package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.otel.SdkTestBase
import io.opentelemetry.api.trace.{Span, SpanKind}
import io.opentelemetry.context.Context
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class TraceSpanTest
    extends AnyFlatSpec
    with should.Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with SdkTestBase {

  "The default context" should "be root" in {
    TraceSpan.context should equal(Context.root())
  }

  "A span" should "exist in local context" in {
    TraceSpan.contextOpt should be(empty)

    TraceSpan.let(Context.root()) {
      TraceSpan.contextOpt should not be empty
      TraceSpan.hasContext should be(true)
      TraceSpan.context should equal(Context.root())

      TraceSpan.span should equal(Span.fromContext(Context.root()))
    }

    TraceSpan.contextOpt should be(empty)
  }

  it should "handle parent conditions" in {
    val builder = TraceSpan.spanBuilderFrom(tracer, SpanKind.INTERNAL)

    // explicitly providing root
    TraceSpan.letChild(Context.root(), builder) {
      TraceSpan.context should not equal Context.root()
      TraceSpan.span should not equal (Span.fromContext(Context.root()))
    }

    // with provided context
    TraceSpan.letChild(TraceSpan.context, builder) {
      TraceSpan.context should not equal Context.root()
      TraceSpan.span should not equal (Span.fromContext(Context.root()))
    }

    // without provided context
    TraceSpan.letChild(builder) {
      TraceSpan.context should not equal Context.root()
      TraceSpan.span should not equal (Span.fromContext(Context.root()))
    }
  }
}
