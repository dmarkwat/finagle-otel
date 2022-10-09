package io.opentelemetry.opencensusshim;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.This;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class Assistant {

    static {
        ByteBuddyAgent.install();
    }

    public Assistant() {
        new ByteBuddy().redefine(OpenTelemetryCtx.class)
                .method(named("getContext"))
                .intercept(MethodDelegation.to(Interceptor.class))
                .make()
                .load(OpenTelemetryCtx.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
    }

    public static class Interceptor {
        private static final Field otelSpanField;

        static {
            try {
                otelSpanField = OpenTelemetrySpanImpl.class.getDeclaredField("otelSpan");
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
            otelSpanField.setAccessible(true);
        }

        public static Context intercept(@This OpenTelemetryCtx otc, @FieldValue Context context) {
            return new Context() {
                @Nullable
                @Override
                public <V> V get(ContextKey<V> key) {
                    return context.get(key);
                }

                @Override
                public <V> Context with(ContextKey<V> k1, V v1) {
                    if (v1 instanceof OpenTelemetrySpanImpl) {
                        Span otelSpan = null;
                        try {
                            otelSpan = (Span) otelSpanField.get(v1);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                        return context.with(otelSpan);
                    }
                    return context.with(k1, v1);
                }
            };
        }
    }
}
