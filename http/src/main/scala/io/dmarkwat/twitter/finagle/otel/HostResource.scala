package io.dmarkwat.twitter.finagle.otel
import io.dmarkwat.twitter.finagle.otel.HostResource.{HostArch, HostName, OsType}
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes

import java.net.InetAddress
import scala.util.Try

trait HostResource extends ResourceIdentity {
  abstract override def resource: Resource = super.resource.merge(
    Map(
      ResourceAttributes.HOST_ARCH -> HostArch,
      ResourceAttributes.HOST_NAME -> HostName,
      ResourceAttributes.OS_TYPE -> OsType
    ).view
      .filter(_._2.isDefined)
      .mapValues(_.get)
      .toMap
      .updated(ResourceAttributes.OS_VERSION, System.getProperty("os.version"))
      .foldLeft(Resource.builder()) { case (resource, (key, value)) =>
        resource.put(key, value)
      }
      .build()
  )
}

object HostResource {
  import ResourceAttributes.{HostArchValues, OsTypeValues}

  lazy val HostName: Option[String] = Try(InetAddress.getLocalHost.getHostName).toOption

  lazy val HostArch: Option[String] = Option(System.getProperty("os.arch") match {
    case "amd64" | "x86_64"  => HostArchValues.AMD64
    case "x86"               => HostArchValues.X86
    case "arm32" | "arm"     => HostArchValues.ARM32
    case "arm64" | "aarch64" => HostArchValues.ARM64
    case "ia64"              => HostArchValues.IA64
    case "ppc32"             => HostArchValues.PPC32
    case "ppc64"             => HostArchValues.PPC64
    case "s390x"             => HostArchValues.S390X
    // omit values which don't conform to the semconv enumeration
    case _ => null
  })

  lazy val OsType: Option[String] = Option(System.getProperty("os.name") match {
    case os if os startsWith "Linux"    => OsTypeValues.LINUX
    case os if os startsWith "Mac OS X" => OsTypeValues.DARWIN
    case os if os startsWith "FreeBSD"  => OsTypeValues.FREEBSD
    case os if os startsWith "NetBSD"   => OsTypeValues.NETBSD
    case os if os startsWith "OpenBSD"  => OsTypeValues.OPENBSD
    case os if os startsWith "Solaris"  => OsTypeValues.SOLARIS
    case os if os startsWith "AIX"      => OsTypeValues.AIX
    case os if os startsWith "HP-UX"    => OsTypeValues.HPUX
    case os if os startsWith "z/OS"     => OsTypeValues.Z_OS
    case os if os startsWith "Windows"  => OsTypeValues.WINDOWS
    // todo dragonfly bsd is unaccounted for
    // omit values which don't conform to the semconv enumeration
    case _ => null
  })
}
