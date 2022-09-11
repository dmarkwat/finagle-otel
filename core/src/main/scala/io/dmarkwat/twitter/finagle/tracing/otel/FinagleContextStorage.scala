package io.dmarkwat.twitter.finagle.tracing.otel

import com.twitter.finagle._
import com.twitter.finagle.context.Contexts
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import io.dmarkwat.twitter.finagle.tracing.otel.FinagleContextStorage.{ContextContainer, NoopScope, ctxKey}
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.{Context, ContextKey, ContextStorage, Scope}

/**
 * Basic emulation of [[io.opentelemetry.context.ThreadLocalContextStorage]]
 * using finagle's [[com.twitter.finagle.context.LocalContext]].
 *
 * Bridges the finagle world with any other non-finagle Otel usages.
 */
class FinagleContextStorage extends ContextStorage with Logging {
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
      trace("stacked attach() calls; returning no op scope")
      NoopScope
    } else {
      trace(s"attaching context ${Span.fromContext(toAttach).getSpanContext}")
      container() = toAttach
      () => {
        if (current() != toAttach) {
          warn(s"mismatched contexts when closing scope; was a context boundary crossed?: current(${Span
            .fromContext(current())
            .getSpanContext});attached(${Span.fromContext(toAttach).getSpanContext})")
        }
        trace(s"closing scope; attaching previous context ${Span.fromContext(before).getSpanContext}")
        container() = before
      }
    }
  }

  // get the current context, or null if we're outside a finagle context boundary -- null is per the interface spec
  override def current(): Context = {
    trace("current from finagle")
    currentOpt().map(_.get).orNull
  }
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

  class ContextWrapper extends Context with Logging {

    val ctx = TraceSpan.context

    override def get[V](key: ContextKey[V]): V = {
      trace("get: hooray")
      TraceSpan.context.get(key)
    }

    override def `with`[V](k1: ContextKey[V], v1: V): Context = {
      trace("with: hooray")
      TraceSpan.context.`with`(k1, v1)
    }
  }

  // add just after any filters that create the requisite context, found in TraceSpan
  class ContextExternalizer[Req, Rep] extends SimpleFilter[Req, Rep] with Logging {
    override def apply(request: Req, service: Service[Req, Rep]): Future[Rep] = {
      // extract the context from the finagle stack and let as a local context value;
      // the externalizer _requires_ the presence of the finagle context for this to be valid and so fails if it's absent
//      val context = TraceSpan.contextOpt.getOrElse(
//        throw new IllegalStateException("trace context isn't present in finagle context")
//      )
      trace("inside")
      val context = TraceSpan.context
      trace(context)
      Contexts.local.let(ctxKey, new ContextContainer(context)) {
        // use makeCurrent instead of directly attaching to the context storage;
        // the interface allows for the possibility this can be different across impls
        val scope = context.makeCurrent()
        service(request) ensure {
          // close, regardless of outcome
//          scope.close()
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
          new ContextExternalizer[Req, Rep].andThen(next)
        }
      }
  }
}
