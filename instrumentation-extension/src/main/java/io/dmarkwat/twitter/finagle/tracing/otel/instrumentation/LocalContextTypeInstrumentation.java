package io.dmarkwat.twitter.finagle.tracing.otel.instrumentation;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Could be used for optimizations on top of or in place of {@link LocalInstanceTypeInstrumentation}.
 */
public class LocalContextTypeInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("com.twitter.finagle.context.LocalContext");
    }

    @Override
    public void transform(TypeTransformer transformer) {
        // deliberately empty for now
    }
}
