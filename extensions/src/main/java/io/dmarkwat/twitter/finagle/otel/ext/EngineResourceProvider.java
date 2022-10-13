package io.dmarkwat.twitter.finagle.otel.ext;

import com.twitter.finagle.InitExtern;
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
                .put(ResourceAttributes.WEBENGINE_VERSION, InitExtern.finagleVersion())
                .build();
        return Resource.create(attrs);
    }
}
