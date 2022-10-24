package io.dmarkwat.twitter.finagle.tracing.otel.instrumentation;

import com.twitter.util.Local;
import io.dmarkwat.twitter.finagle.tracing.otel.TraceScoping;
import io.dmarkwat.twitter.finagle.tracing.otel.TraceSpan;
import io.dmarkwat.twitter.finagle.tracing.otel.Traced;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Instruments the finagle library according to the top level {@link Traced} returned from {@link Traced#get()}.
 * <p>
 * In all likelihood and cases, this will be {@link TraceSpan}, however, no chance is taken.
 */
public class LocalInstanceTypeInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        // interested in the `object` instance specifically
        return named("com.twitter.util.Local$");
    }

    // required methods to set Context.current():
    //  - restore
    //  - let
    //  - closed
    //
    // deliberately omitted:
    //  - clear: handled by restore (empty context is passed)
    //  - letClear: handled by let (empty context is passed)

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                isMethod()
                        .and(named("restore"))
                        .and(takesArguments(1))
                        .and(takesArgument(0, named("com.twitter.util.Local$Context"))),
                LocalInstanceTypeInstrumentation.class.getName() + "$HandleRestoreAdvice");

        transformer.applyAdviceToMethod(
                isMethod()
                        .and(named("let"))
                        .and(takesArguments(2))
                        .and(takesArgument(0, named("com.twitter.util.Local$Context"))),
                LocalInstanceTypeInstrumentation.class.getName() + "$HandleLetAdvice");

        transformer.applyAdviceToMethod(
                isMethod()
                        .and(named("closed"))
                        .and(takesArguments(1)),
                LocalInstanceTypeInstrumentation.class.getName() + "$HandleClosedAdvice");

        transformer.applyAdviceToMethod(
                isMethod()
                        .and(named("letClear"))
                        .and(takesArguments(1)),
                LocalInstanceTypeInstrumentation.class.getName() + "$HandleLetClearAdvice");
    }

    public static class HandleRestoreAdvice {

        // don't care about argument since finagle hides away the local Key we need to get at the underlying Context value
        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void restoreExit(@Advice.Argument(value = 0) Local.Context finContext) {
            if (finContext == Local.Context$.MODULE$.empty()) {
                // empty context means the finagle-traced context is cleared; use root context
                Java8BytecodeBridge.rootContext().makeCurrent();
                return;
            }
            // todo optimize
            scala.Option<Context> inFinagle = Traced.get().contextOpt();
            if (inFinagle.isDefined()) {
                // actively ignore the returned Scope and don't do any closing;
                // it's assumed this is all being handled by the user-space code;
                // this merely ensures the current context is assigned in the java agent upon finagle context switching
                inFinagle.get().makeCurrent();
            }
        }
    }

    public static class HandleLetAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void letEnter(@Advice.Argument(value = 0) Local.Context finContext,
                                    @Advice.Argument(value = 1, readOnly = false) scala.Function0<?> fn) {
            // defer obtaining and making the otel context current;
            // works because finContext will be assigned as the active context at the time the function is called;
            // so no extra work required
            fn = TraceScoping.extern$.MODULE$.wrapping(TraceSpan.context(), fn);
        }
    }

    public static class HandleClosedAdvice {
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void closedEnter(@Advice.Argument(value = 0, readOnly = false) scala.Function0<?> fn) {
            // create closure around current context;
            // obtains a reference to the current context's otel context, preserving semantics of Local::closed
            fn = TraceScoping.extern$.MODULE$.wrapping(TraceSpan.context(), fn);
        }
    }

    public static class HandleLetClearAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void letClearEnter(@Advice.Argument(value = 0, readOnly = false) scala.Function0<?> fn) {
            // use the root context;
            // with all locals cleared, there is no context to use: set it to root
            fn = TraceScoping.extern$.MODULE$.wrapping(Context.root(), fn);
        }
    }
}
