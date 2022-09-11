package io.dmarkwat.twitter.finagle.tracing.otel.instrumentation;

import com.twitter.util.Local;
import io.dmarkwat.twitter.finagle.tracing.otel.TraceSpan;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import scala.runtime.AbstractFunction0;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static net.bytebuddy.matcher.ElementMatchers.*;

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
                        .and(takesArgument(0, named("com.twitter.util.Local$Context")))
                // shrug: no idea why this isn't working -- might need to be java.lang.Void?
//                        .and(returns(named("scala.Unit$")))
                ,
                this.getClass().getName() + "$HandleRestoreAdvice");

        transformer.applyAdviceToMethod(
                isMethod()
                        .and(named("let"))
                        .and(takesArguments(2))
                        .and(takesArgument(0, named("com.twitter.util.Local$Context")))
                ,
                this.getClass().getName() + "$HandleLetAdvice");
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
            scala.Option<Context> inFinagle = TraceSpan.contextOpt();
            if (inFinagle.isDefined()) {
                System.out.println("restoring");
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
                                    @Advice.Argument(value = 1, readOnly = false) scala.Function0<?> fn,
                                    @Advice.Origin Method method) {
            System.out.println("here i am 2: " + Modifier.isFinal(method.getParameters()[1].getModifiers()));
            // wrap the function
            fn = io.dmarkwat.twitter.finagle.tracing.otel.Helper.wrapFunction(fn);

            // todo figure out why the extension hates the java-agent-local Helper
            // the otel extension setup does NOT like this helper class...
            // the 2 helper classes are identical;
            // if i use the local one the advice is never applied;
            // if i use the one in the dependency, it works just fine...
            // fn = Helper.wrapFunction(fn);
        }
    }

    public static class HandleClosedAdvice {
    }
}
