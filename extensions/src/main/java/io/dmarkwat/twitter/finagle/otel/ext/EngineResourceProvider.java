package io.dmarkwat.twitter.finagle.otel.ext;

import io.dmarkwat.twitter.finagle.tracing.otel.ext.BuildInfo;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

public class EngineResourceProvider implements ResourceProvider {
    @Override
    public Resource createResource(ConfigProperties config) {
        Attributes attrs = Attributes.builder()
                .put(ResourceAttributes.WEBENGINE_NAME, "finagle")
                .put(ResourceAttributes.WEBENGINE_VERSION, BuildInfo.instance.finagleVersion)
                .build();
        return Resource.create(attrs);
    }
}
