package io.dmarkwat.twitter.finagle.otel

import io.opentelemetry.sdk.OpenTelemetrySdk

trait SdkBootstrap {
  self: SdkTraceBootstrap =>

  OpenTelemetrySdk.builder().setTracerProvider(self.traceProvider).buildAndRegisterGlobal()
}
