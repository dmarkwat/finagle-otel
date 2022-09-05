package io.dmarkwat.twitter.finagle.test

import com.twitter.finagle.http.{Method, Response}
import com.twitter.finagle.service.ExpiringService
import com.twitter.finagle.tracing.TraceInitializerFilter
import com.twitter.finagle.{Http, RefusedByRateLimiter, Service, Status, http}
import com.twitter.util.Future
import io.dmarkwat.twitter.finagle.tracing.otel.{HttpClientTraceSpanInitializer, HttpClientTracer, HttpServerTraceSpanInitializer, HttpServerTracer}

import scala.util.Try

class IntegrationTest {
//  def clientDef(): Http.Client = Http.client
//    .withTracer(new HttpClientTracer)
//    .withStack(stack =>
//      stack
//        .insertAfter(ExpiringService.role, MutatingRateLimitingFilter.module[http.Request, http.Response](strategy))
//        .insertAfter(
//          TraceInitializerFilter.role,
//          new HttpClientTraceSpanInitializer[http.Request, http.Response](otelTracer)
//        )
//    )
//
//  val client = clientDef().newService(s":${55555}")
//
//  object IntBox {
//    def unapply(s: String): Option[Int] = Try(s.toInt).toOption
//  }
//
//  clientDef().newClient("")
//
//  val service = new Service[http.Request, http.Response] {
//    def apply(req: http.Request): Future[http.Response] = {
//      val hopsLeft = req.contentString match {
//        case ""        => 0
//        case IntBox(i) => i
//      }
//      if (hopsLeft > 0) {
//        val next = http.Request(Method.Get, "/")
//        next.contentString = (hopsLeft - 1).toString
//        clientDef().newService(s":${55555}")(next).map(rep => http.Response(req.version, rep.status)) handle {
//          case RefusedByRateLimiter() => Response.apply(Status.TooManyRequests)
//        }
//      } else {
//        Future.value {
//          val rep = http.Response(req.version, http.Status.Ok)
//          rep.contentString = hopsLeft.toString
//          rep
//        }
//      }
//    }
//  }
//  Http.server
//    .withTracer(new HttpServerTracer)
//    .withStack(stack =>
//      stack
//        .insertAfter(
//          TraceInitializerFilter.role,
//          new HttpServerTraceSpanInitializer[http.Request, http.Response](otelTracer)
//        )
//    )
//    .serve(s":${55555}", service)
}
