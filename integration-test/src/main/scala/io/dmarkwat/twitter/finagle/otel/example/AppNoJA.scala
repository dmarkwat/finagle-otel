package io.dmarkwat.twitter.finagle.otel.example

import com.twitter.app
import com.twitter.finagle.Http
import com.twitter.util.Await
import com.twitter.util.logging.Logging
import io.dmarkwat.twitter.finagle.otel._
import io.dmarkwat.twitter.finagle.tracing.otel.ContextStorageProvider
import io.dmarkwat.twitter.finale.otel.example.BuildInfo
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.`export`.{SimpleSpanProcessor, SpanExporter}
import io.opentelemetry.sdk.trace.data.SpanData

import java.util

object AppNoJA
    extends app.App
    with BaseApp
    with ResourceIdentity
    with ServiceResource
    with K8sResource
    //    with ProcessResource
    with HostResource
    with SdkTraceBootstrap
    with SdkBootstrap
    with SdkBootstrap.Globalized
    with ContextStorageProvider.WrappingContextStorage
    with Logging {

  override val serviceName: String = sys.env.getOrElse("OTEL_SERVICE_NAME", "na")
  override val serviceNamespace: String = "ns"
  override val serviceInstanceId: String = "instance-0"
  override val serviceVersion: String = BuildInfo.version

  override lazy val traceSpanProcessors: List[SpanProcessor] =
    super.traceSpanProcessors ++ List(SimpleSpanProcessor.create(new SpanExporter {
      override def `export`(spans: util.Collection[SpanData]): CompletableResultCode = {
        spans.forEach { s =>
          debug(s)
        }
        CompletableResultCode.ofSuccess()
      }

      override def flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

      override def shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
    }))

  def main(): Unit = {
    init()

    import com.twitter.finagle.OtelImplicits._

    val server = Http.server
      .withOtel(otelTracer)
      .serve(
        s"localhost:${port()}",
        new SimpleService(mkDbClient)
      )

    Await.ready(server)
  }
}
