package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.http.Request
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.util.Future

/**
 * Squeezes as much extra metadata out of requests as possible for the otel integration;
 * whatever the built-in finagle configurations don't emit by default or otherwise reliably.
 *
 * @tparam Req
 * @tparam Rep
 */
class OtelExtractor[Req <: Request, Rep] extends SimpleFilter[Req, Rep] {

  override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
    request.host.foreach(KnownAnnotations.recordHostHeader)

    // can't extract http scheme from here due to how finagle represents things

    service(request)
  }
}
