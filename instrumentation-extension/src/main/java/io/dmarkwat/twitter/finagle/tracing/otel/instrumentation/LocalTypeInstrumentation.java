package io.dmarkwat.twitter.finagle.tracing.otel.instrumentation;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.named;

// not required based on current implementation
public class LocalTypeInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("com.twitter.util.Local");
    }

    // set Context.current() for methods:
    //
    // required but otherwise handled:
    //  - let(): handled by LocalContextTypeInstrumentation
    //  - letClearAll(): handled by LocalContextTypeInstrumentation
    //
    // deliberately omitted, but would require interception under different circumstances:
    //  - update(T): never called by LocalContext
    //  - set(Option[T]): never called by LocalContext
    //  - clear(): never called by LocalContext

    @Override
    public void transform(TypeTransformer transformer) {
    }
}
