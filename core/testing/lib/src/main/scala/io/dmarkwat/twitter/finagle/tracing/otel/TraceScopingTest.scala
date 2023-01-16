package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.util.Await
import io.dmarkwat.twitter.finagle.otel.{ExecutorSupport, SdkProvider, SdkTestCase}
import io.dmarkwat.twitter.finagle.tracing.otel.Implicits.RichContext
import io.dmarkwat.twitter.finagle.{BaseTestSpec, PoolSupport1}
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TraceScopingTest
    extends BaseTestSpec
    with BeforeAndAfterEach
    with SdkProvider.Library
    with PoolSupport1
    with ExecutorSupport
    with ExplicitStorage {

  "Some function" should "use current" in new SdkTestCase {
    TraceScoping.extern.makeCurrent(TraceSpan.context) {
      TraceSpan.context should equal(root)
    }

    TraceScoping.extern.makeCurrent(TraceSpan.context) {
      TraceScoping.extern.makeCurrent(TraceSpan.context) {
        TraceSpan.context should equal(root)
      }
    }
  }

  it should "be wrapped" in new SdkTestCase {
    TraceScoping.extern.wrapping(root) {
      TraceSpan.context should equal(root)
    }

    val thread = Thread.currentThread()
    val ctx = this.randomContext
    val fn = TraceScoping.extern.wrapping(ctx) {
      thread should not equal Thread.currentThread()
      current().asSpan should equal(ctx.asSpan)
    }

    current().asSpan should equal(root.asSpan)
    Await.result(primary { fn.apply() })
  }
}
