package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.tracing.Annotation._
import com.twitter.finagle.tracing.{AnnotatingTracingFilter, Record, TraceId, Tracer}
import io.opentelemetry.api.common.{AttributeKey, Attributes}
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes._

import java.net.InetSocketAddress
import scala.util.matching.Regex

object HttpTracer {

  /**
   * @see [[AnnotatingTracingFilter.afterFailure]] and its usage for the format in question
   */
  private val traceInitializerFailureMessageRegex: Regex = """^(?<typeName>.*): (?<message>.*)$""".r
}

/**
 * Wires up the finagle [[Tracer]] interface for http tracing backed by Otel entirely on finagle [[com.twitter.finagle.context.Contexts]].
 *
 * @see [[https://github.com/openzipkin/zipkin-finagle/blob/master/core/src/main/java/zipkin2/finagle/SpanRecorder.java zipkin impl reference]]
 * @see [[https://opentelemetry.io/docs/reference/specification/trace/sdk_exporters/zipkin/ translation ref]]
 */
abstract class HttpTracer extends Tracer {

  val serviceNameAttr: AttributeKey[String]
  val reqPayloadSizeTraceKey: String
  val repPayloadSizeTraceKey: String
  def clientAddressAttrs(ia: InetSocketAddress): Attributes
  def serverAddressAttrs(ia: InetSocketAddress): Attributes
  def statusClassifier(status: Int): StatusCode

  /**
   * Annotations mapped from finagle to otel (using zipkin's interpretation which is as close to finagle-native as it gets).
   *
   * Links and trace state are marked as TBD ATTOW.
   *
   * @see [[https://opentelemetry.io/docs/reference/specification/trace/sdk_exporters/zipkin/#summary mapping ref]]
   */
  override def record(record: Record): Unit = record match {
    // trace id and duration don't concern us: otel does things differently from finagle
    case Record(_, timestamp, annotation, _) =>
      //
      // todo HTTP_HOST attribute is never set anywhere in finagle -- kind of needs to be!
      //
      annotation match {
        // otel: Span.Name; zipkin: Span.name
        case Rpc(name) =>
          TraceSpan.span.updateName(name)
        // otel attributes: peer.service and/or rpc.service
        //  peer.service => client attribute
        //  rpc service => server attribute
        // handled by impls
        case ServiceName(service) =>
          TraceSpan.span.setAttribute(serviceNameAttr, service)
        //
        // otel network-level attributes
        //
        case ClientAddr(ia) =>
          TraceSpan.span.setAllAttributes(clientAddressAttrs(ia))
        case ServerAddr(ia) =>
          TraceSpan.span.setAllAttributes(serverAddressAttrs(ia))
        //
        // no specific handling
        //        case Annotation.LocalAddr(ia) =>
        //
        // awkward case
        case err @ (ClientRecvError(_) | ServerSendError(_)) =>
          def handle(error: String): Unit = {
            val HttpTracer.traceInitializerFailureMessageRegex(typeName, message) = error
            TraceSpan.span.addEvent(
              EXCEPTION_EVENT_NAME,
              Attributes
                .of(EXCEPTION_TYPE, typeName, EXCEPTION_MESSAGE, message),
              timestamp.toInstant
            )
          }
          err match {
            case ClientRecvError(error) => handle(error)
            case ServerSendError(error) => handle(error)
          }
        //
        // various binary annotations mapped to sometimes-complex modifications
        //
        // see ClientExceptionTracingFilter
        // starts the event recording machinery: finagle's in-tree exception Recording is a multi-Record process
        case BinaryAnnotation("error", true) =>
          TraceSpan.eventing(_.newException())
          // todo find a way to determine which errors are terminal so we can set the span status correctly?
          //  however, presumably, if the span eventually succeeds it will yield an http status which will overwrite the status...
          TraceSpan.span.setStatus(StatusCode.ERROR)
        case BinaryAnnotation("exception.type", typeName: String) =>
          TraceSpan.eventing(_.withAttribute(EXCEPTION_TYPE, typeName))
        case BinaryAnnotation("exception.message", message: String) =>
          TraceSpan.eventing(_.withAttribute(EXCEPTION_MESSAGE, message).record(timestamp))
        //
        // not an event or complex collection of items; just set the attributes
        //
        case BinaryAnnotation(key, value) =>
          (key, value) match {
            // http: HttpTracingFilter
            case ("http.method", method: String) => TraceSpan.span.setAttribute(HTTP_METHOD, method)
            case ("http.uri", uri: String)       => TraceSpan.span.setAttribute(HTTP_URL, uri)
            case ("http.status_code", status: Int) =>
              TraceSpan.span
                .setStatus(statusClassifier(status))
                .setAttribute(HTTP_STATUS_CODE.asInstanceOf[AttributeKey[Long]], status.toLong)
            // DestinationTracing
            case ("clnt/finagle.protocol", protocol: String) =>
              // todo also need to figure out how to get the http scheme
              protocol match {
                case "http" =>
                  TraceSpan.span
                    .setAttribute(HTTP_FLAVOR, HttpFlavorValues.HTTP_1_1)
                    .setAttribute(NET_TRANSPORT, NetTransportValues.IP_TCP)
                case "http/2" =>
                  TraceSpan.span
                    .setAttribute(HTTP_FLAVOR, HttpFlavorValues.HTTP_2_0)
                    // don't forget: this is UDP for http/3 (when finagle supports it)
                    .setAttribute(NET_TRANSPORT, NetTransportValues.IP_TCP)
                case _ => // unknown
              }
            //
            // finagle PayloadSizeFilter
            case (`reqPayloadSizeTraceKey`, size: Int) =>
              TraceSpan.span.setAttribute(
                HTTP_REQUEST_CONTENT_LENGTH.asInstanceOf[AttributeKey[Long]],
                size.toLong
              )
            case (`repPayloadSizeTraceKey`, size: Int) =>
              TraceSpan.span.setAttribute(
                HTTP_RESPONSE_CONTENT_LENGTH.asInstanceOf[AttributeKey[Long]],
                size.toLong
              )
            // todo http PayloadSizeFilter? they handle chunked requests and responses
            case _ =>
          }
        //
        // could turn this trio into an event
        //
        //        case Annotation.WireSend             =>
        //        case Annotation.WireRecv             =>
        //        case Annotation.WireRecvError(error) =>
        //
        // unused atm
        //
        //        case Annotation.ClientSendFragment     =>
        //        case Annotation.ClientRecvFragment     =>
        //        case Annotation.ServerSendFragment     =>
        //        case Annotation.ServerRecvFragment     =>
        //        case Annotation.Message(content)       =>
        case _ =>
      }
  }

  // always sample when working with otel -- it will make the decisions
  override def sampleTrace(traceId: TraceId): Option[Boolean] = Tracer.SomeTrue
}
