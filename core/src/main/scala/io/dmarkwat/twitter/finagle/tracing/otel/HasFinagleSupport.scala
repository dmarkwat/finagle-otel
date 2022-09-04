package io.dmarkwat.twitter.finagle.tracing.otel

import io.opentelemetry.context.ContextStorage

/**
 * Trait used as a marker denoting the [[ContextStorage]] supports finagle.
 * It enforces nothing, but merely makes startup-time checks for supported context storage possible.
 */
trait HasFinagleSupport {
  self: ContextStorage =>
}
