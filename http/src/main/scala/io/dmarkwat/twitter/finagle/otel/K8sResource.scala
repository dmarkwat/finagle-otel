package io.dmarkwat.twitter.finagle.otel
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes

trait K8sResource extends ResourceIdentity {

  abstract override def resource: Resource = super.resource.merge(
    Resource
      .builder()
      .put(ResourceAttributes.K8S_POD_UID, sys.env.get("K8S_POD_UID").orNull)
      .put(ResourceAttributes.K8S_POD_NAME, sys.env.get("K8S_POD_NAME").orNull)
      .put(ResourceAttributes.K8S_CONTAINER_NAME, sys.env.get("K8S_CONTAINER_NAME").orNull)
      .put(ResourceAttributes.K8S_NAMESPACE_NAME, sys.env.get("K8S_NAMESPACE_NAME").orNull)
      .put(ResourceAttributes.K8S_NODE_NAME, sys.env.get("K8S_NODE_NAME").orNull)
      .build()
  )
}
