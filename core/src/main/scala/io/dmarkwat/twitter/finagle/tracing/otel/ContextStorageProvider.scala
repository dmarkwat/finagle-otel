package io.dmarkwat.twitter.finagle.tracing.otel

import io.opentelemetry.context
import io.opentelemetry.context.{Context, ContextStorage, Scope}

// prefer to use WrappingContextStorage as it sanely implements safety mechanisms for handling
// cases when the finagle context isn't set (think libraries doing out-of-band work)
class ContextStorageProvider extends context.ContextStorageProvider {
  override def get(): ContextStorage = new FinagleContextStorage
}

object ContextStorageProvider {

  // import me to add the wrapper to ContextStorage
  //
  // affords the ability to avoid setting the global storage provider, instead opting for a wrapper
  // when the otel finagle tracing is present on the finagle stack.
  //
  // allows for mixing in non-finagle usages without burdening implementations from obtaining finagle context
  // when they're executed or operating outside finagle itself (e.g. a thread pool that starts outside a finagle context
  // which performs continuous async work, exclusive of any finagle activity).
  //
  // alternatively, simply wrapping the executor at the top level or wrapping the startup of the service with the finagle
  // context may also work.
  object WrappingContextStorage {
    ContextStorage.addWrapper(storage => {
      val finagleStorage = new FinagleContextStorage
      new ContextStorage {
        override def attach(toAttach: Context): Scope = {
          // if the context isn't present, we're presumed to be outside finagle: use the fallback
          if (finagleStorage.currentOpt().isDefined) {
            finagleStorage.attach(toAttach)
          } else {
            storage.attach(toAttach)
          }
        }

        // if the finagle context isn't set, fallback to the previous storage
        override def current(): Context = finagleStorage.currentOpt().map(_.get).getOrElse(storage.current())
      }
    })
  }
}
