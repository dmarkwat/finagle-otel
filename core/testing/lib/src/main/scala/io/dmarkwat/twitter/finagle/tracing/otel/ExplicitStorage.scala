package io.dmarkwat.twitter.finagle.tracing.otel

import io.dmarkwat.twitter.finagle.otel.SdkProvider
import io.opentelemetry.context

trait ExplicitStorage {
  self: SdkProvider =>

  override implicit def storageProvider: context.ContextStorageProvider = () => new ContextStorage
}
