# Finagle OpenTelemetry integration

This is a rough cut of opentelemetry integration for Finagle.

## Current Status

Working.
Major workaround for javaagent exists.

However, test coverage is far from complete and mostly manual at the moment.

Outliers and known issues:
- opencensus-shim misbehaves: scoped spans are not made correctly active upon first entry into the opencensus shimming from the otel world -- the otel span remains the current span despite the scoping -- and result in premature closure of the otel span
  - this is the direct result of the otel java agent requiring a very specific implementation of `Span` (`ApplicationSpan`) to be passed along to its internals; when it isn't, issues like this one occur
  - why it happens: opencensus shim simply passes along its own `OpenTelemetrySpanImpl` (which _wraps_ this special `ApplicationSpan`) assuming all is well; ordinarily, the interface is enough as tests _without_ the java agent work perfectly well; but _with_ the java agent, the correct behavior breaks down and fails because java agent is looking for that _very specific_ type
  - see
    - `io.opentelemetry.opencensusshim.OpenTelemetryContextManager::withValue`
    - `io.opentelemetry.javaagent.instrumentation.opentelemetryapi.context.AgentContextWrapper`'s `ContextKeyBridge` for the SPAN key
    - `io.opentelemetry.javaagent.instrumentation.opentelemetryapi.trace.Bridging#toAgentOrNull(Span)` for where the previous obtains the reference and ultiamtely where the behavior breaks down

Workarounds:
- through the use of ByteBuddy (same as used by the otel agent), an interceptor can be created which correctly extracts the internal `ApplicationSpan` from the `OpenTelemetrySpanImpl` and passes it along to the correct `Context` method, averting the issue and correcting the problem
  - this absolutely needs to be patched into either the otel agent OR the shim
  - attempts were made with the otel agent but to no avail: the very specific needs of the java agent are a bit overwhelming when interacting with the instrumented `application...Context` vs the javaagent-facing regular `Context` and needs to be done in-tree to get the shaded `application.*` classes, complicating everything further

## Reproducing and using

Regardless of IDE used, check the `.run` directory for working run configurations (some setup required due to the gradle project & otel java agent extension).
These files contain env vars, paths, etc. for wiring up the sample, `App`.
If in IntelliJ, this file is importable and will work from the `Run configurations...` menu.

In IntelliJ:
1. Build `instrumentation-extension`: `gradle build`
2. Download the otel java agent jar: `curl -Lo opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.18.0/opentelemetry-javaagent.jar`
3. Start spanner emulator: `gcloud emulators spanner start`
   - this listens on a stable host:port and is embedded in the committed run configuration
4. Run/Debug `Run configurations...` > `App w/javaagent`
5. `curl http://localhost:9999/`

Upon running, the otel logging configuration emits the traces to stderr in the IntelliJ console.
Trace IDs should be stable across all the activities, and the span name, operation name, attributes, etc. should all be correctly emitted on the final span emitted from this project's finagle filter & tracer.

Examples (see above for known issues):
```
[otel.javaagent 2022-10-01 11:34:51:231 -0600] [finagle/netty4-2-1] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET' : aead9da3e74d1e20e20e356181fd9347 67cd17692e6ff92e SERVER [tracer: finagle-otel:] AttributesMap{data={net.peer.name=localhost, net.peer.port=47340, http.method=GET, http.request_content_length=0, net.host.name=localhost, http.status_code=200, rpc.service=my-service, net.host.port=9999, http.url=/, thread.id=93, thread.name=finagle/netty4-2-1, net.peer.ip=127.0.0.1, net.host.ip=127.0.0.1, http.host=localhost:9999}, capacity=128, totalAddedValues=14}
```
