package io.dmarkwat.twitter.finagle.tracing.otel.instrumentation;

import io.dmarkwat.twitter.finagle.tracing.otel.TraceSpan;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import scala.Function0;
import scala.runtime.AbstractFunction0;

public final class Helper {

    public static <T> scala.Function0<T> wrapFunction(scala.Function0<T> fn) {
        return new AbstractFunction0<T>() {
            @Override
            public T apply() {
                Context inFinagle = TraceSpan.contextOpt().getOrElse(new Function0<Context>() {
                    @Override
                    public Context apply() {
                        return Java8BytecodeBridge.rootContext();
                    }
                });
                Scope ignored = inFinagle.makeCurrent();
                try {
                    return fn.apply();
                } finally {
                    ignored.close();
                }
            }
        };
    }
}
