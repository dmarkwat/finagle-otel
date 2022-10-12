package io.dmarkwat.twitter.finagle.otel.example

import com.twitter.app
import com.twitter.finagle.Http
import com.twitter.util.Await
import com.twitter.util.logging.Logging
import io.dmarkwat.twitter.finagle.otel.SdkBootstrap
import io.dmarkwat.twitter.finagle.tracing.otel._

object App
    extends app.App
    with BaseApp
    with SdkBootstrap.Auto
    with ContextStorageProvider.WrappingContextStorage
    with Logging {

  def main(): Unit = {
    init()

    import com.twitter.finagle.OtelImplicits._

    val server = Http.server
      .withOtel()
      .serve(
        s"localhost:${port()}",
        new SimpleService(mkDbClient)
      )

    Await.ready(server)
  }
}
