package io.dmarkwat.twitter.finagle.otel

import com.twitter.finagle.InitExtern
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.jdk.CollectionConverters.MapHasAsScala

class ResourceIdentityTest extends AnyFlatSpec with should.Matchers {
  "A baseline ResourceIdentity" should "return the default" in {
    new Object with ResourceIdentity {}.resource should be(Resource.getDefault)
  }

  "A stacked ResourceIdentity" should "return all stacked attributes" in {
    val stacked = new Object
      with ResourceIdentity
      with ServiceResource
      with K8sResource
      with ProcessResource
      with HostResource {
      override val serviceName: String = "my-service"
      override val serviceNamespace: String = "my-ns"
      override val serviceInstanceId: String = "an-instance-id"
      override val serviceVersion: String = "0.0.0-alpha"
    }.resource.getAttributes.asMap().asScala

    stacked.keys should contain allOf (
      // host resource
      HOST_ARCH, HOST_NAME, OS_TYPE, OS_VERSION,
      // service resource
      SERVICE_NAME, SERVICE_NAMESPACE, SERVICE_INSTANCE_ID, SERVICE_VERSION, WEBENGINE_NAME, WEBENGINE_VERSION,
      // process resource
      PROCESS_PID, PROCESS_RUNTIME_NAME, PROCESS_RUNTIME_VERSION, PROCESS_OWNER, PROCESS_COMMAND, PROCESS_COMMAND_ARGS, PROCESS_COMMAND_LINE,
      // defaults from otel
      // see Resource.getDefault()
      TELEMETRY_SDK_NAME, TELEMETRY_SDK_LANGUAGE, TELEMETRY_SDK_VERSION
    )

    stacked should contain(SERVICE_NAME -> "my-service")
    stacked should contain(SERVICE_NAMESPACE -> "my-ns")
    stacked should contain(SERVICE_INSTANCE_ID -> "an-instance-id")
    stacked should contain(SERVICE_VERSION -> "0.0.0-alpha")
    stacked should contain(WEBENGINE_NAME -> "finagle")
    stacked should contain(WEBENGINE_VERSION -> InitExtern.finagleVersion())
  }
}
