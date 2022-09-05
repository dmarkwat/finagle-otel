package io.dmarkwat.twitter.finagle.test

import com.twitter.conversions.DurationOps.richDurationFromInt
import com.twitter.finagle._
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.tracing.TraceInitializerFilter
import com.twitter.finagle.util.Rng
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Await, Closable, Future}
import io.dmarkwat.twitter.finagle.otel.{ResourceIdentity, SdkBootstrap, SdkTraceBootstrap}
import io.dmarkwat.twitter.finagle.tracing.otel.{HttpClientTraceSpanInitializer, HttpClientTracer, HttpServerTraceSpanInitializer, HttpServerTracer}
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.`export`.{SimpleSpanProcessor, SpanExporter}
import org.mockito.invocation.InvocationOnMock
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{Format, Json}

class PingPongIT extends AnyFlatSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with MockitoSugar {
  val mockedSpanExporter = mock[SpanExporter]
  Mockito
    .doAnswer((invocation: InvocationOnMock) => {
      println(invocation.getArgument(0))
      CompletableResultCode.ofSuccess()
    })
    .when(mockedSpanExporter)
    .`export`(ArgumentMatchers.any())
  Mockito.doReturn(CompletableResultCode.ofSuccess()).when(mockedSpanExporter).flush()
  Mockito.doReturn(CompletableResultCode.ofSuccess()).when(mockedSpanExporter).shutdown()
  Mockito.doNothing().when(mockedSpanExporter).close()

  val app = new Object with ResourceIdentity with SdkTraceBootstrap with SdkBootstrap {
    override lazy val traceSpanProcessors: List[SpanProcessor] = SimpleSpanProcessor.create(mockedSpanExporter) :: Nil
  }

  val pingAddr = s":${45454}"
  val pongAddr = s":${54545}"

  var pingServer: ListeningServer = _
  var pongServer: ListeningServer = _

  case class Serve(unused: String)

  case object Serve {
    implicit val format: Format[Serve] = Json.format[Serve]
  }

  case class Game(hits: Int) {
    def hit(): Game = copy(hits = hits + 1)
  }

  object Game {
    implicit val format: Format[Game] = Json.format[Game]

    def apply(): Game = Game(0)
  }

  case class Match(winner: String, hits: Int)

  object Match {
    implicit val format: Format[Match] = Json.format[Match]
  }

  def opponentClient(tracer: Tracer): Http.Client = Http.client
    .withTracer(new HttpClientTracer)
    .withStack(stack =>
      stack
        .insertAfter(
          TraceInitializerFilter.role,
          new HttpClientTraceSpanInitializer[http.Request, http.Response](tracer)
        )
    )

  override def beforeAll(): Unit = {
    def pingPongService(chanceToHit: Double, _client: => Http.Client, peer: String): Service[Request, Response] = {
      require(chanceToHit > 0 && chanceToHit < 1)

      val opponent = _client.newService(peer)
      val rng = Rng()

      (req: http.Request) => {
        req.path match {
          case "/serve" =>
            Json.parse(req.contentString).as[Serve]
            opponent(
              http.Request(
                req.version,
                Method.Post,
                "/hit",
                Reader.fromBuf(Buf.ByteArray(Json.toBytes(Json.toJson(Game())): _*))
              )
            )
          case "/hit" =>
            val game = Json.parse(req.contentString).as[Game]
            if (chanceToHit > rng.nextDouble()) {
              opponent(
                http.Request(
                  req.version,
                  Method.Post,
                  "/hit",
                  Reader.fromBuf(Buf.ByteArray(Json.toBytes(Json.toJson(game.hit())): _*))
                )
              )
            } else {
              Future.value(
                http.Response(
                  req.version,
                  http.Status.Ok,
                  Reader.fromBuf(Buf.ByteArray(Json.toBytes(Json.toJson(Match(peer, game.hits))): _*))
                )
              )
            }
        }
      }
    }

    pingServer = Http.server
      .withTracer(new HttpServerTracer)
      .withStack(stack =>
        stack
          .insertAfter(
            TraceInitializerFilter.role,
            new HttpServerTraceSpanInitializer[http.Request, http.Response](app.otelTracer)
          )
      )
      .serve(pingAddr, pingPongService(0.8d, opponentClient(app.otelTracer), pongAddr))

    pongServer = Http.server
      .withTracer(new HttpServerTracer)
      .withStack(stack =>
        stack
          .insertAfter(
            TraceInitializerFilter.role,
            new HttpServerTraceSpanInitializer[http.Request, http.Response](app.otelTracer)
          )
      )
      .serve(pongAddr, pingPongService(0.8d, opponentClient(app.otelTracer), pingAddr))
  }

  override def afterAll(): Unit = {
    Closable.all(pingServer, pongServer).close()
  }

  "A ping pong server" should "play a game" in {
    val client = opponentClient(app.otelTracer).newService(pingAddr)

    val matchDetails = Await.result(
      client(
        http.Request(
          http.Version.Http11,
          Method.Post,
          "/serve",
          Reader.fromBuf(Buf.ByteArray(Json.toBytes(Json.toJson(Serve(""))): _*))
        )
      ).map(rep => Json.parse(rep.contentString).as[Match]),
      5.seconds
    )

    val expectedSpans = {
      // each hit is 2 spans
      matchDetails.hits * 2 +
        // the request to serve is 2 spans (client and server)
        2 +
        // the losing side has one span
        1 +
        // the root span
        1
    }

    Mockito.verify(mockedSpanExporter, Mockito.times(expectedSpans)).`export`(ArgumentMatchers.any())

    Closable.all(client).close()
  }
}
