package io.dmarkwat.twitter.finagle.otel

import com.twitter.finagle.InitExtern
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes

trait ServiceResource extends ResourceIdentity {
  self: ResourceIdentity =>

  val serviceName: String
  val serviceNamespace: String
  val serviceInstanceId: String
  val serviceVersion: String

  abstract override def resource: Resource = super.resource.merge(
    Resource
      .builder()
      .put(ResourceAttributes.SERVICE_NAME, serviceName)
      .put(ResourceAttributes.SERVICE_NAMESPACE, serviceNamespace)
      .put(ResourceAttributes.SERVICE_INSTANCE_ID, serviceInstanceId)
      .put(ResourceAttributes.SERVICE_VERSION, serviceVersion)
      .put(ResourceAttributes.WEBENGINE_NAME, "finagle")
      .put(ResourceAttributes.WEBENGINE_VERSION, InitExtern.finagleVersion())
      .build()
  )
}
