# Finagle OpenTelemetry integration

## Wiring

If deploying in k8s, when wiring the `OpenTelemetrySdk` with an `SdkTracer` built with `SdkTracerProvider`, set the Resource using `.setResource(...)` according to a combination of deployment/build info and namespace:
- `io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME` (`service.name`): the logical name of the service (`:<process>`, such as `:java` suffix not strictly required)
- `io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAMESPACE` (`service.namespace`): the logical namespace the service belongs to; in k8s this might be the namespace (use env fieldRef or Downward API to expose)
  - could also be some other string if services aren't deployed in k8s namespaces along this same dimension
- `io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_INSTANCE_ID` (`service.instance.id`): the unique identity of the running service; in k8s this could be a Pod's name or uid
  - point-in-time identity is probably fine (e.g. pod name), but universally unique (e.g. pod uid) may be beneficial in the long run
- `io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_VERSION` (`service.version`): the version of the running service
  - endow the artifact with "build info" or expose the version via env vars in k8s, for example

See [here](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/semantic_conventions/README.md#service) for detailed info.
