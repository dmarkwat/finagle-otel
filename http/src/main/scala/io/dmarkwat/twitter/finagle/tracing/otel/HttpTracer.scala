package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.tracing.Annotation._
import com.twitter.finagle.tracing.{AnnotatingTracingFilter, Annotation, Tracer}
import com.twitter.util.logging.Logging
import io.dmarkwat.twitter.finagle.tracing.otel.KnownAnnotations.{HostHeader, HostHeaderKey, UriScheme, UriSchemeKey}
import io.dmarkwat.twitter.finagle.tracing.otel.OtelTracer.TraceMeta
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
 * Base http tracer implementation shared by server and client sides to enforce otel's standardization.
 *
 * @see [[https://github.com/openzipkin/zipkin-finagle/blob/master/core/src/main/java/zipkin2/finagle/SpanRecorder.java zipkin impl reference]]
 * @see [[https://opentelemetry.io/docs/reference/specification/trace/sdk_exporters/zipkin/ translation ref]]
 */
abstract class HttpTracer extends OtelTracer {
  self: Logging =>

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
  override def annotationHandlers: PartialFunction[Annotation, Unit] = super.annotationHandlers orElse {
    // http: HttpTracingFilter
    case BinaryAnnotation("http.method", method: String) => TraceSpan.span.setAttribute(HTTP_METHOD, method)
    case BinaryAnnotation("http.uri", uri: String)       => TraceSpan.span.setAttribute(HTTP_URL, uri)
    case BinaryAnnotation("http.status_code", status: Int) =>
      TraceSpan.span
        .setStatus(statusClassifier(status))
        .setAttribute(HTTP_STATUS_CODE.asInstanceOf[AttributeKey[Long]], status.toLong)
    // DestinationTracing
    case BinaryAnnotation("clnt/finagle.protocol", protocol: String) =>
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
    // various binary annotations mapped to sometimes-complex modifications
    //
    case BinaryAnnotation(HostHeaderKey, HostHeader(host)) => TraceSpan.span.setAttribute(HTTP_HOST, host)
    case BinaryAnnotation(UriSchemeKey, UriScheme(scheme)) => TraceSpan.span.setAttribute(HTTP_SCHEME, scheme)
    //
    // otel network-level attributes
    //
    case ClientAddr(ia) =>
      TraceSpan.span.setAllAttributes(clientAddressAttrs(ia))
    case ServerAddr(ia) =>
      TraceSpan.span.setAllAttributes(serverAddressAttrs(ia))
    //
    // finagle PayloadSizeFilter
    case BinaryAnnotation(`reqPayloadSizeTraceKey`, size: Int) =>
      TraceSpan.span.setAttribute(
        HTTP_REQUEST_CONTENT_LENGTH.asInstanceOf[AttributeKey[Long]],
        size.toLong
      )
    case BinaryAnnotation(`repPayloadSizeTraceKey`, size: Int) =>
      TraceSpan.span.setAttribute(
        HTTP_RESPONSE_CONTENT_LENGTH.asInstanceOf[AttributeKey[Long]],
        size.toLong
      )
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
          TraceMeta.get.timestamp.toInstant
        )
      }

      err match {
        case ClientRecvError(error) => handle(error)
        case ServerSendError(error) => handle(error)
      }
    // todo http PayloadSizeFilter? they handle chunked requests and responses
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
  }
}
