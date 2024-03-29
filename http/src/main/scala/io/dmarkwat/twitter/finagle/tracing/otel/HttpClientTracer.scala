package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.filter.PayloadSizeFilter.{ClientRepTraceKey, ClientReqTraceKey}
import com.twitter.util.logging.Logging
import io.opentelemetry.api.common.{AttributeKey, Attributes}
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes._

import java.net.InetSocketAddress

/**
 * Client-side http tracer.
 *
 * @see [[https://opentelemetry.io/docs/reference/specification/trace/sdk_exporters/zipkin/ reference]]
 * @see [[https://github.com/openzipkin/zipkin-finagle/blob/master/core/src/main/java/zipkin2/finagle/SpanRecorder.java zipkin impl reference]]
 */
class HttpClientTracer extends HttpTracer with Logging {

  override val serviceNameAttr: AttributeKey[String] = SemanticAttributes.PEER_SERVICE
  override val reqPayloadSizeTraceKey: String = ClientReqTraceKey
  override val repPayloadSizeTraceKey: String = ClientRepTraceKey

  // all 400 errors are client errors;
  // assertion: if the server failed, so did the client, regardless of if the client handled it gracefully
  // todo is ^^^ correct?
  override def statusClassifier(status: Int): StatusCode = if (status < 400) StatusCode.OK else StatusCode.ERROR

  override def clientAddressAttrs(ia: InetSocketAddress): Attributes = Attributes.of(
    NET_SOCK_HOST_ADDR,
    ia.getAddress.getHostAddress,
    NET_HOST_NAME,
    ia.getHostName,
    NET_HOST_PORT.asInstanceOf[AttributeKey[Long]],
    ia.getPort.toLong
  )

  override def serverAddressAttrs(ia: InetSocketAddress): Attributes = Attributes.of(
    NET_SOCK_PEER_ADDR,
    ia.getAddress.getHostAddress,
    NET_PEER_NAME,
    ia.getHostName,
    NET_PEER_PORT.asInstanceOf[AttributeKey[Long]],
    ia.getPort.toLong
  )
}
