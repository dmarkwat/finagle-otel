package io.dmarkwat.twitter.finagle.tracing.otel.instrumentation;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;

import java.util.Arrays;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class LocalInstrumentationModule extends InstrumentationModule {

    public LocalInstrumentationModule() {
        super("finagle", "finagle-22.7");
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return Arrays.asList(new LocalInstanceTypeInstrumentation(), new TraceSpanInstrumentation(), new LocalContextTypeInstrumentation());
    }

    @Override
    public List<String> getAdditionalHelperClassNames() {
        return List.of("io.dmarkwat.twitter.finagle.tracing.otel.instrumentation.Helper");
    }
}
