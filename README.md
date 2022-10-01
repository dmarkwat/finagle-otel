# Finagle OpenTelemetry integration

This is a rough cut of opentelemetry integration for Finagle.

## Current Status

Working.

However, test coverage is far from complete and mostly manual at the moment.

Outliers and known issues:
- opencensus-shim seems to misbehave: scoped spans are not made correctly active upon first entry into the opencensus shimming from the otel world -- the otel span remains the current span despite the scoping -- and result in premature closure of the otel span
  - and, when a new span is created for the opencensus code path to "consume" in this way, the spans don't record and emit

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
