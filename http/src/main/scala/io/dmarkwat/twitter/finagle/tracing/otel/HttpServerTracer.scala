package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.filter.PayloadSizeFilter.{ServerRepTraceKey, ServerReqTraceKey}
import com.twitter.finagle.tracing.{TraceId, Tracer}
import io.opentelemetry.api.common.{AttributeKey, Attributes}
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes._

import java.net.InetSocketAddress

class HttpServerTracer extends HttpTracer {

  override val serviceNameAttr: AttributeKey[String] = RPC_SERVICE
  override val reqPayloadSizeTraceKey: String = ServerReqTraceKey
  override val repPayloadSizeTraceKey: String = ServerRepTraceKey

  // 400 series errors are all client side and therefore don't impact the server's trace status:
  // it still behaved normally and is in the realm of "expected" for the server itself
  // todo is ^^^ accurate?
  override def statusClassifier(status: Int): StatusCode = if (status < 500) StatusCode.OK else StatusCode.ERROR

  override def clientAddressAttrs(ia: InetSocketAddress): Attributes = Attributes.of(
    NET_PEER_IP,
    ia.getAddress.getHostAddress,
    NET_PEER_NAME,
    ia.getHostName,
    NET_PEER_PORT.asInstanceOf[AttributeKey[Long]],
    ia.getPort.toLong
  )

  override def serverAddressAttrs(ia: InetSocketAddress): Attributes = Attributes.of(
    NET_HOST_IP,
    ia.getAddress.getHostAddress,
    NET_HOST_NAME,
    ia.getHostName,
    NET_HOST_PORT.asInstanceOf[AttributeKey[Long]],
    ia.getPort.toLong
  )

  // always sample when working with this tracer
  override def sampleTrace(traceId: TraceId): Option[Boolean] = Tracer.SomeTrue
}
