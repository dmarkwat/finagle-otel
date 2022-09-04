package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle._
import com.twitter.finagle.context.Contexts
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import io.dmarkwat.twitter.finagle.tracing.otel.FinagleContextStorage.{ContextContainer, NoopScope, ctxKey}
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.{Context, ContextStorage, Scope}

/**
 * Basic emulation of [[io.opentelemetry.context.ThreadLocalContextStorage]]
 * using finagle's [[com.twitter.finagle.context.LocalContext]].
 *
 * Bridges the finagle world with any other non-finagle Otel usages.
 */
class FinagleContextStorage extends ContextStorage with Logging with HasFinagleSupport {
  def currentOpt(): Option[ContextContainer] = Contexts.local.get(ctxKey)

  override def attach(toAttach: Context): Scope = {
    assert(toAttach != null)

    // we must be inside a finagle context boundary to attach context
    val container = currentOpt().getOrElse(
      throw new IllegalStateException(
        "misconfigured stack: can't attach the current context without a context container in finagle local context"
      )
    )
    val before = container.get
    // same context -- do nothing as it's presumed to be a stacked call
    if (before == toAttach) {
      NoopScope
    } else {
      container() = toAttach
      () => {
        if (current() != toAttach) {
          warn(s"mismatched contexts when closing scope; was a context boundary crossed?: current(${Span
            .fromContext(current())
            .getSpanContext});attached(${Span.fromContext(toAttach).getSpanContext})")
        }
        container() = before
      }
    }
  }

  // get the current context, or null if we're outside a finagle context boundary -- null is per the interface spec
  override def current(): Context = currentOpt().map(_.get).orNull
}

object FinagleContextStorage {
  val ctxKey: Contexts.local.Key[ContextContainer] = Contexts.local.newKey[ContextContainer]()

  object NoopScope extends Scope {
    override def close(): Unit = {}
  }

  final class ContextContainer(private var context: Context) {
    // current otel context as viewed by the current service stack
    def get: Context = context

    def update(ctx: Context): Unit = context = ctx
  }

  // add just after any filters that create the requisite context, found in TraceSpan
  class ContextExternalizer[Req, Rep] extends SimpleFilter[Req, Rep] {
    override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
      // extract the context from the finagle stack and let as a local context value;
      // the externalizer _requires_ the presence of the finagle context for this to be valid and so fails if it's absent
      val context = TraceSpan.contextOpt.getOrElse(
        throw new IllegalStateException("trace context isn't present in finagle context")
      )
      Contexts.local.let(ctxKey, new ContextContainer(context)) {
        // use makeCurrent instead of directly attaching to the context storage;
        // the interface allows for the possibility this can be different across impls
        val scope = context.makeCurrent()
        service(request) ensure {
          // close, regardless of outcome
          scope.close()
        }
      }
    }
  }

  object ContextExternalizer {
    val role: Stack.Role = Stack.Role("FinagleContextStorageExternalizer")

    def module[Req, Rep]: Stackable[ServiceFactory[Req, Rep]] =
      new Stack.Module0[ServiceFactory[Req, Rep]] {
        val role: Stack.Role = ContextExternalizer.role
        val description = ""

        def make(
            next: ServiceFactory[Req, Rep]
        ): ServiceFactory[Req, Rep] = {
          if (!ContextStorage.get().isInstanceOf[HasFinagleSupport]) {
            throw new RuntimeException("context storage must support finagle for this filter to work")
          }
          new ContextExternalizer[Req, Rep].andThen(next)
        }
      }
  }
}
