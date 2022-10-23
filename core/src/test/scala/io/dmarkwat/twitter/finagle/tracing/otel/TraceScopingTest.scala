package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.util.Await
import io.dmarkwat.twitter.finagle.PoolSupport1
import io.dmarkwat.twitter.finagle.otel.{ExecutorSupport, SdkProvider, SdkTestCase}
import io.dmarkwat.twitter.finagle.tracing.otel.Implicits._
import io.opentelemetry.api.trace.Span
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

class TraceScopingTest
    extends AnyFlatSpec
    with should.Matchers
    with BeforeAndAfterEach
    with SdkProvider.Library
    with PoolSupport1
    with ExecutorSupport {

  "Some function" should "use current" in new SdkTestCase {
    TraceScoping.makeCurrent(TraceSpan.context) {
      TraceSpan.context should equal(root)
    }

    TraceScoping.makeCurrent(TraceSpan.context) {
      TraceScoping.makeCurrent(TraceSpan.context) {
        TraceSpan.context should equal(root)
      }
    }
  }

  it should "be wrapped" in new SdkTestCase {
    TraceScoping.wrapping(root) {
      TraceSpan.context should equal(root)
    }

    val thread = Thread.currentThread()
    val ctx = this.randomContext
    val fn = TraceScoping.wrapping(ctx) {
      thread should not equal Thread.currentThread()
      Span.current() should equal(ctx.asSpan)
    }

    Span.current() should equal(root.asSpan)
    Await.result(primary { fn.apply() })
  }
}
