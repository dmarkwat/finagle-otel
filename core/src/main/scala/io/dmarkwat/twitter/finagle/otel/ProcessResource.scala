package io.dmarkwat.twitter.finagle.otel

import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes

import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.jdk.OptionConverters.RichOptional

trait ProcessResource extends ResourceIdentity {

  abstract override def resource: Resource = super.resource.merge(
    Resource
      .builder()
      .put(ResourceAttributes.PROCESS_PID, java.lang.Long.valueOf(ProcessHandle.current().pid()))
      .put(ResourceAttributes.PROCESS_RUNTIME_NAME, System.getProperty("java.runtime.name"))
      .put(ResourceAttributes.PROCESS_RUNTIME_VERSION, System.getProperty("java.runtime.version"))
      .put(ResourceAttributes.PROCESS_OWNER, ProcessHandle.current().info().user().toScala.orNull)
      .put(ResourceAttributes.PROCESS_COMMAND, ProcessHandle.current().info().command().toScala.orNull)
      .put(
        ResourceAttributes.PROCESS_COMMAND_ARGS,
        ProcessHandle.current().info().arguments().toScala.map(_.toList.asJava).orNull
      )
      .put(
        ResourceAttributes.PROCESS_COMMAND_LINE,
        ProcessHandle.current().info().commandLine().toScala.orNull
      )
      .build()
  )
}
