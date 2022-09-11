package io.dmarkwat.twitter.finagle.tracing.otel;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import scala.runtime.AbstractFunction0;

public final class Helper {

    public static <T> scala.Function0<T> wrapFunction(scala.Function0<T> fn) {
        return new AbstractFunction0<T>() {
            @Override
            public T apply() {
                Context inFinagle = TraceSpan.contextOpt().getOrElse(Context::root);
                try (Scope ignored = inFinagle.makeCurrent()) {
                    return fn.apply();
                }
            }
        };
    }
}
