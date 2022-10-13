package io.dmarkwat.twitter.finagle.otel.ext;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public class K8sResourceProvider implements ResourceProvider {
    @SuppressWarnings({"ConstantConditions"})
    @Override
    public Resource createResource(ConfigProperties config) {
        Attributes attributes = Attributes
                .builder()
                .put(ResourceAttributes.K8S_POD_UID, config.getString("K8S_POD_UID"))
                .put(ResourceAttributes.K8S_POD_NAME, config.getString("K8S_POD_NAME"))
                .put(ResourceAttributes.K8S_CONTAINER_NAME, config.getString("K8S_CONTAINER_NAME"))
                .put(ResourceAttributes.K8S_NAMESPACE_NAME, config.getString("K8S_NAMESPACE_NAME"))
                .put(ResourceAttributes.K8S_NODE_NAME, config.getString("K8S_NODE_NAME"))
                .build();
        return Resource.create(attributes);
    }
}
