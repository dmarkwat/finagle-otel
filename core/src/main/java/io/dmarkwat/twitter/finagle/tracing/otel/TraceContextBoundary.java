package io.dmarkwat.twitter.finagle.tracing.otel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for the method/s used to propagate an otel Context into finagle's local Context.
 * <p>
 * Used in instrumentation to easily identify what otherwise might be a moving target across releases.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface TraceContextBoundary {
}
