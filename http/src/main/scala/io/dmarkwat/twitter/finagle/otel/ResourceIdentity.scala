package io.dmarkwat.twitter.finagle.otel

import io.opentelemetry.sdk.resources.Resource

trait ResourceIdentity {
  def resource: Resource = Resource.getDefault
}
