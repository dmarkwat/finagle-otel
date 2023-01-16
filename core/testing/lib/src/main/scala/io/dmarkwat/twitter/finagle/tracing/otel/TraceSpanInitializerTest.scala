package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.{Filter, Service}
import com.twitter.util.{Await, Future}
import io.dmarkwat.twitter.finagle.BaseTestSpec
import io.dmarkwat.twitter.finagle.otel.{SdkProvider, SdkTestCase}
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.propagation.{TextMapGetter, TextMapPropagator, TextMapSetter}
import org.junit.runner.RunWith
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatestplus.junit.JUnitRunner

import java.lang
import scala.jdk.CollectionConverters.IterableHasAsJava

@RunWith(classOf[JUnitRunner])
class TraceSpanInitializerTest extends BaseTestSpec with TableDrivenPropertyChecks with SdkProvider.Library {

  val propagators: TableFor1[TextMapPropagator] = Table(
    "p",
    TextMapPropagator.noop()
    // todo implement in the http module tests
//    W3CTraceContextPropagator.getInstance()
  )

  val spanTest: Service[Span, Span] = Service.mk { (parent: Span) =>
    TraceSpan.span should not equal parent
    TraceSpan.span.getSpanContext should not equal parent.getSpanContext
    TraceSpan.span.getSpanContext.isValid should be(true)
    TraceSpan.span.isRecording should be(true)

    Future.value(TraceSpan.span)
  }

  def testFilter(sdk: SdkTestCase, filter: Filter[Span, Span, Span, Span]): Unit = {
    val mockedRoot = sdk.randomContext

    val service: Service[Span, Span] = filter.andThen(spanTest)

    val child = Await result TraceSpan.let(mockedRoot) {
      service.apply(TraceSpan.span)
    }

    child.isRecording should be(false)
    child.getSpanContext.isValid should be(true)

    val span = Span.fromContext(mockedRoot)

    span.isRecording should be(true)
    span.getSpanContext.isValid should be(true)
  }

  "A server filter" should "exist" in new SdkTestCase {
    TraceSpanInitializer.server(
      tracer,
      TextMapPropagator.noop(),
      new TextMapGetter[Object] {
        override def keys(carrier: Object): lang.Iterable[String] = ???

        override def get(carrier: Object, key: String): String = ???
      }
    )
  }

  forAll(propagators) { propagator =>
    it should s"propagate ${propagator.getClass} correctly" in new SdkTestCase {
      testFilter(
        this,
        TraceSpanInitializer
          .server(
            tracer,
            propagator,
            new TextMapGetter[Span] {
              override def keys(carrier: Span): lang.Iterable[String] = {
                println("get keys")
                Iterable.empty.asJava
              }

              override def get(carrier: Span, key: String): String = {
                key
              }
            }
          )
      )
    }
  }

  "A client filter" should "exist" in new SdkTestCase {
    TraceSpanInitializer
      .client(
        tracer,
        TextMapPropagator.noop(),
        new TextMapSetter[Span] {
          override def set(carrier: Span, key: String, value: String): Unit = {}
        }
      )
  }

  forAll(propagators) { propagator =>
    it should s"propagate ${propagator.getClass} correctly" in new SdkTestCase {
      testFilter(
        this,
        TraceSpanInitializer
          .client(
            tracer,
            propagator,
            new TextMapSetter[Span] {
              override def set(carrier: Span, key: String, value: String): Unit = {}
            }
          )
      )
    }
  }
}
