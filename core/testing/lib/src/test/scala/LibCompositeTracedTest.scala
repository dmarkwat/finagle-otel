import io.dmarkwat.twitter.finagle.otel.SdkProvider
import io.dmarkwat.twitter.finagle.tracing.otel.CompositeTracedTest
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class LibCompositeTracedTest extends CompositeTracedTest with SdkProvider.Library {}
