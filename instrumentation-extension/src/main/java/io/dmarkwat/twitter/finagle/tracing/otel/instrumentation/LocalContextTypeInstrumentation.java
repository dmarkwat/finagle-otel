package io.dmarkwat.twitter.finagle.tracing.otel.instrumentation;

import com.twitter.finagle.context.Context.KeyValuePair;
import com.twitter.finagle.context.Contexts$;
import com.twitter.finagle.context.LocalContext;
import io.dmarkwat.twitter.finagle.tracing.otel.TraceSpan$;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import scala.collection.Iterable;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class LocalContextTypeInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("com.twitter.finagle.context.LocalContext");
    }

    // set Context.current() for methods:
    //  - letClearAll(): need to handle this case since it cuts across the entire local context unlike the other methods
    //
    // omitted:
    //  - let (all variations): specializing on instrumenting TraceSpan directly
    //  - letClear (both variations): never called by TraceSpan

    //
    // todo need to make the same method for all different Local methods to push or pop the current context into/out of place as Context.current()
    //

    @Override
    public void transform(TypeTransformer transformer) {
        transformer.applyAdviceToMethod(
                isMethod()
                        // only one letClearAll defined
                        .and(named("letClearAll")),
                this.getClass().getName() + "$HandleLetClearAllAdvice");

//        transformer.applyAdviceToMethod(
//                isMethod()
//                        .and(named("let"))
//                        .and(takesArguments(3))
//                        // scala -> java making my head spin; need the $ instead of . for the type name match
//                        .and(takesArgument(0, named("com.twitter.finagle.context.LocalContext$Key")))
//                        // but don't try to inject this into the method advice as an argument
//                        .and(takesArgument(2, hasSuperType(named("scala.Function0"))))
//                ,
//                this.getClass().getName() + "$HandleLet_SingleAdvice");
//

        //
        // technically only let(key, val)(fn) and letClearAll(fn) are required bc those are the only ones used by or otherwise affect TraceSpan
        //

//        transformer.applyAdviceToMethod(
//                isMethod()
//                        .and(named("let"))
//                        .and(takesArguments(5))
//                        .and(takesArgument(0, named("com.twitter.finagle.context.LocalContext$Key")))
//                        .and(takesArgument(2, named("com.twitter.finagle.context.LocalContext$Key")))
//                        .and(takesArgument(4, hasSuperType(named("scala.Function0"))))
//                ,
//                this.getClass().getName() + "$HandleLet_MultiAdvice");
//
//        transformer.applyAdviceToMethod(
//                isMethod()
//                        .and(named("let"))
//                        .and(takesArguments(2))
//                        .and(takesArgument(0, named("com.twitter.finagle.context.LocalContext$Key")))
//                        .and(takesArgument(1, hasSuperType(named("scala.Function0"))))
//                ,
//                this.getClass().getName() + "$HandleLet_IterableAdvice");
    }

    public static class HandleLetClearAllAdvice {

        // set the context before executing the inner function
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void restoreContext(@Advice.This Object self) {
            // clearing out the context means no active otel Context
            Java8BytecodeBridge.rootContext().makeCurrent();
        }
    }

    public static class HandleLet_SingleAdvice {

        // set the context before executing the inner function
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void restoreContext(@Advice.This Object self,
                                          @Advice.Argument(value = 0) LocalContext.Key<?> key,
                                          @Advice.Argument(value = 1) Object obj) {
            // only interested in the global Contexts.local()
            if (self != Contexts$.MODULE$.local()) {
                return;
            }
            // only interested if the key is the context key
            if (key != TraceSpan$.MODULE$.contextKey()) {
                return;
            }
            System.out.println("setting context");
            // must be a context by virtue of it being the correct key
            ((Context) obj).makeCurrent();
        }
    }

    public static class HandleLet_MultiAdvice {
        // set the context before executing the inner function
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void restoreContext(@Advice.This Object self,
                                          @Advice.Argument(value = 0) LocalContext.Key<?> key1,
                                          @Advice.Argument(value = 1) Object obj1,
                                          @Advice.Argument(value = 0) LocalContext.Key<?> key2,
                                          @Advice.Argument(value = 1) Object obj2) {
            // only interested in the global Contexts.local()
            if (self != Contexts$.MODULE$.local()) {
                return;
            }
            // only interested if the key is the context key
            if (key1 == TraceSpan$.MODULE$.contextKey()) {
                System.out.println("setting context");
                // must be a context by virtue of it being the correct key
                ((Context) obj1).makeCurrent();
            } else if (key2 == TraceSpan$.MODULE$.contextKey()) {
                // must be a context by virtue of it being the correct key
                ((Context) obj2).makeCurrent();
            }
            // neither matched -- do nothing
        }
    }

    public static class HandleLet_IterableAdvice {
        // set the context before executing the inner function
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void restoreContext(@Advice.This Object self,
                                          @Advice.Argument(value = 0) Iterable<KeyValuePair<?>> kvs) {
            // todo
        }
    }

    public static class HandleLetClear_SingleAdvice {
    }

    public static class HandleLetClear_IterableAdvice {

    }
}
