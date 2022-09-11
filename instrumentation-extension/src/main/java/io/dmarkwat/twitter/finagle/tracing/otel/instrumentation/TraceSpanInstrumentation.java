package io.dmarkwat.twitter.finagle.tracing.otel.instrumentation;

import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

// todo consider doing away with this in favor of exporting "manually" to allow seamless manual/auto instrumentation
public class TraceSpanInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("io.dmarkwat.twitter.finagle.tracing.otel.TraceSpan$");
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                isMethod()
                        // add annotation to the method to make this sane?
                        .and(isAnnotatedWith(named("io.dmarkwat.twitter.finagle.tracing.otel.TraceContextBoundary"))),
                this.getClass().getName() + "$HandleTraceSpanAdvice");
    }

    public static class HandleTraceSpanAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void letContext(@Advice.AllArguments Object[] args) {
            System.out.println("here i am");
            // javaagent classpath trickery fools Advice into thinking AgentContextWrapper isn't a Context when it is
            Context ctx = (Context) args[0];
            // make the in-finagle context the current context in the otel world
            ctx.makeCurrent();
        }
    }
}
