import com.twitter.app
import com.twitter.util.logging.Logging
import io.opencensus.trace.{AttributeValue, Status, Tracing}
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind

import java.io.Closeable
import java.time.Instant
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.Using

object Regression extends app.App with Logging {
  otel()

  def otel(): Unit = {
    val span = GlobalOpenTelemetry.getTracer("test").spanBuilder("testSpan").setSpanKind(SpanKind.INTERNAL).startSpan()
    Using.resource(span.makeCurrent()) { _ =>
      info("doing opencensus work")
      doOpenCensus()
      info("done")
    }
    span.setAttribute("test", 123)
    span.end()
  }

  private def doOpenCensus(): Unit = {
    val tracer = Tracing.getTracer
    try {
      val span = tracer.spanBuilder("main").startSpan()
      Using(tracer.withSpan(span)) { _ =>
        Tracing.getTracer.getCurrentSpan.putAttributes(Map("interesting" -> AttributeValue.stringAttributeValue("things")).asJava)
        info("About to do some busy work...")
        for (i <- 0 until 1) {
          doWork(i)
        }
      }
    }
  }

  private def doWork(i: Int): Unit = { // 6. Get the global singleton Tracer object.
    val tracer = Tracing.getTracer
    // 7. Start another span. If another span was already started, it'll use that span as the parent span.
    // In this example, the main method already started a span, so that'll be the parent span, and this will be
    // a child span.
    try {
      val scope = tracer.spanBuilder("doWork").startScopedSpan
      try { // Simulate some work.
        val span = tracer.getCurrentSpan
        try {
          info("doing busy work")
          Thread.sleep(100L)
        } catch {
          case e: InterruptedException =>
            // 6. Set status upon error
            span.setStatus(Status.INTERNAL.withDescription(e.toString))
        }
        // 7. Annotate our span to capture metadata about our operation
        val attributes = Map("use" -> AttributeValue.stringAttributeValue("demo"))
        span.addAnnotation("Invoking doWork", attributes.asJava)
      } finally if (scope != null) scope.close()
    }
  }
}
