package io.dmarkwat.twitter.finagle.otel.example

import com.twitter.app
import com.twitter.finagle.Http
import com.twitter.util.Await
import com.twitter.util.logging.Logging
import io.dmarkwat.twitter.finagle.otel.SdkBootstrap

object App extends app.App with BaseApp with SdkBootstrap.Auto with Logging {

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
