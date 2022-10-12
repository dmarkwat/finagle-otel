package com.twitter.finagle

import com.twitter.finagle.Http.{Client, Server}
import com.twitter.finagle.tracing.TraceInitializerFilter
import io.dmarkwat.twitter.finagle.otel.example.App.otelTracer
import io.dmarkwat.twitter.finagle.tracing.otel.FinagleContextStorage.ContextExternalizer
import io.dmarkwat.twitter.finagle.tracing.otel.{HttpClientTraceSpanInitializer, HttpServerTraceSpanInitializer, HttpServerTracer}

object OtelImplicits {
  implicit class RichHttpServer(server: Server) {

    def withOtel(): Server = {
      server
        .withLabel(sys.env.getOrElse("OTEL_SERVICE_NAME", "unknown"))
        .withTracer(new HttpServerTracer)
        .withStack(stack =>
          stack
            .insertAfter(
              TraceInitializerFilter.role,
              ContextExternalizer.module[http.Request, http.Response]
            )
            .insertAfter(
              TraceInitializerFilter.role,
              new HttpServerTraceSpanInitializer[http.Request, http.Response](otelTracer)
            )
        )
    }
  }

  implicit class RichHttpClient(client: Client) {

    def withOtel(): Client = {
      client
        // todo extract from the client in use
//        .withLabel()
        .withStack(stack =>
          stack
            // todo don't think this is required
//            .insertAfter(
//              TraceInitializerFilter.role,
//              ContextExternalizer.module[http.Request, http.Response]
//            )
            .insertAfter(
              TraceInitializerFilter.role,
              new HttpClientTraceSpanInitializer[http.Request, http.Response](otelTracer)
            )
        )
    }
  }
}
