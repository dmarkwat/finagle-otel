package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.filter.PayloadSizeFilter.{ClientRepTraceKey, ClientReqTraceKey}
import com.twitter.finagle.tracing.{TraceId, Tracer}
import io.opentelemetry.api.common.{AttributeKey, Attributes}
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes

import java.net.InetSocketAddress

// this is for clients: server will have an inverted setup due to how spans are constructed and worked on
//
// ref: https://opentelemetry.io/docs/reference/specification/trace/sdk_exporters/zipkin/
// zipkin impl reference: https://github.com/openzipkin/zipkin-finagle/blob/master/core/src/main/java/zipkin2/finagle/SpanRecorder.java
class HttpClientTracer extends HttpTracer {

  override val serviceNameAttr: AttributeKey[String] = SemanticAttributes.PEER_SERVICE
  override val reqPayloadSizeTraceKey: String = ClientReqTraceKey
  override val repPayloadSizeTraceKey: String = ClientRepTraceKey

  // all 400 errors are client errors;
  // assertion: if the server failed, so did the client, regardless of if the client handled it gracefully
  // todo is ^^^ correct?
  override def statusClassifier(status: Int): StatusCode = if (status < 400) StatusCode.OK else StatusCode.ERROR

  override def clientAddressAttrs(ia: InetSocketAddress): Attributes = Attributes.of(
    SemanticAttributes.NET_HOST_IP,
    ia.getAddress.getHostAddress,
    SemanticAttributes.NET_HOST_NAME,
    ia.getHostName,
    SemanticAttributes.NET_HOST_PORT.asInstanceOf[AttributeKey[Long]],
    ia.getPort.toLong
  )

  override def serverAddressAttrs(ia: InetSocketAddress): Attributes = Attributes.of(
    SemanticAttributes.NET_HOST_IP,
    ia.getAddress.getHostAddress,
    SemanticAttributes.NET_HOST_NAME,
    ia.getHostName,
    SemanticAttributes.NET_HOST_PORT.asInstanceOf[AttributeKey[Long]],
    ia.getPort.toLong
  )

  // always sample when working with this tracer
  override def sampleTrace(traceId: TraceId): Option[Boolean] = Tracer.SomeTrue
}
