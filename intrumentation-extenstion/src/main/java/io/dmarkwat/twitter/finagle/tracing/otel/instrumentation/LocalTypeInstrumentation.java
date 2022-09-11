package io.dmarkwat.twitter.finagle.tracing.otel.instrumentation;

import com.twitter.finagle.context.Contexts;
import io.dmarkwat.twitter.finagle.tracing.otel.TraceSpan;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class LocalTypeInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        // interested in the `object` instance specifically
        return named("com.twitter.util.Local$");
    }

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                isMethod()
                        .and(named("restore"))
                        .and(takesArguments(1))
//                        .and(takesArgument(0, named("com.twitter.util.Local$Context")))
//                        .and(returns(named("scala.Unit")))
                ,
                this.getClass().getName() + "$HandleLocalAdvice");
    }

    public static class HandleLocalAdvice {

        // don't care about argument since finagle hides away the local Key we need to get at the underlying Context value
        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void restoreContext() {
            // todo optimize
            scala.Option<Context> inFinagle = TraceSpan.contextOpt();
            if (inFinagle.isDefined()) {
                System.out.println("current: " + Context.current());
                System.out.println("restoring: " + inFinagle.get());
                // actively ignore the returned Scope and don't do any closing;
                // it's assumed this is all being handled by the user-space code;
                // this merely ensures the current context is assigned in the java agent upon finagle context switching
                inFinagle.get().makeCurrent();
            } else {
                System.out.println("restoring no context");
            }
        }
    }
}
