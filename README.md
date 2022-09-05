# Finagle OpenTelemetry integration

## Quick test

1. make assembly (`sbt integrationTest/assembly`)
2. start datadog (or any other OTLP exporter) in background: `docker run --network host --rm -ti -e DD_API_KEY=<key> -e DD_OTLP_CONFIG_RECEIVER_PROTOCOLS_GRPC_ENDPOINT=0.0.0.0:4317 datadog/agent:7.38.2`
3. obtain jar ([here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases))
4. start jar: `OTEL_SERVICE_NAME=my-service OTEL_EXPORTER_OTLP_PROTOCOL=grpc OTEL_EXPORTER_OTLP_ENDPOINT=http://`hostname`:4317 java -javaagent:$HOME/Downloads/opentelemetry-javaagent.jar -jar ./integration-test/target/scala-2.13/integration-test-assembly-0.1.0-SNAPSHOT.jar`
5. make requests and watch the traces flow
