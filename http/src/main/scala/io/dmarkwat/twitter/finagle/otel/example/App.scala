package io.dmarkwat.twitter.finagle.otel.example

import com.twitter.app
import com.twitter.app.Flag
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.tracing.TraceInitializerFilter
import com.twitter.finagle.{Http, http}
import com.twitter.util.logging.Logging
import com.twitter.util.{Await, Future}
import io.dmarkwat.twitter.finagle.otel.SdkBootstrap
import io.dmarkwat.twitter.finagle.tracing.otel.FinagleContextStorage.ContextExternalizer
import io.dmarkwat.twitter.finagle.tracing.otel.{ContextStorageProvider, HttpServerTraceSpanInitializer, HttpServerTracer}

object App extends app.App with SdkBootstrap.Auto with ContextStorageProvider.WrappingContextStorage with Logging {

  val port: Flag[Int] = flag("p", 9999, "port")

  eagerInit

  def main(): Unit = {

    val server = Http.server
      .withTracer(new HttpServerTracer)
      .withStack(stack =>
        stack
          .insertAfter(
            TraceInitializerFilter.role,
            new HttpServerTraceSpanInitializer[http.Request, http.Response](otelTracer)
          )
          .insertAfter(
            HttpServerTraceSpanInitializer.role,
            ContextExternalizer.module[http.Request, http.Response]
          )
      )
      .serve(
        s"localhost:${port()}",
        (req: Request) => {
          info("logged")
          Future.value(Response())
        }
      )

    Await.ready(server)
  }
}
