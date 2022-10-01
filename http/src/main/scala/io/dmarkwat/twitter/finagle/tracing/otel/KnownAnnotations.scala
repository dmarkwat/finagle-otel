package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle.tracing.Trace

object KnownAnnotations {

  private val keyPrefix = "otel"

  private def mkKey(suffix: String) = {
    s"$keyPrefix/$suffix"
  }

  val HostHeaderKey: String = mkKey("hostHeader")
  val UriSchemeKey: String = mkKey("uriScheme")

  sealed trait BinaryAnnotationValue

  case class HostHeader(host: String) extends BinaryAnnotationValue
  case class UriScheme(scheme: String) extends BinaryAnnotationValue

  def recordHostHeader(host: String): Unit = Trace.recordBinary(HostHeaderKey, HostHeader(host))
  def recordUriScheme(scheme: String): Unit = Trace.recordBinary(UriSchemeKey, UriScheme(scheme))
}
