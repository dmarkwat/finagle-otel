package io.dmarkwat.twitter.finagle.tracing.otel;

import io.opentelemetry.context.Context;
import scala.runtime.AbstractFunction0;

public final class Helper {

    private Helper() {
    }

    public static <T> scala.Function0<T> wrapFunction(scala.Function0<T> fn) {
        return new AbstractFunction0<>() {
            @Override
            public T apply() {
                return TraceScoping$.MODULE$.makeCurrent(TraceSpan.contextOpt().getOrElse(Context::root), fn);
            }
        };
    }
}
