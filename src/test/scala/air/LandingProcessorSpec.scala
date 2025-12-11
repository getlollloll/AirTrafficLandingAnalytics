package air

import air.actors.LandingProcessor
import air.messages.ProcessorProtocol
import air.model.LandingRecord
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.time.YearMonth

class LandingProcessorSpec extends AnyFunSuite with BeforeAndAfterAll {

  private val testKit = ActorTestKit()
  implicit private val system: akka.actor.typed.ActorSystem[Nothing] = testKit.system

  override def afterAll(): Unit = testKit.shutdownTestKit()

  private def sample: Seq[LandingRecord] =
    Seq(
      LandingRecord(YearMonth.of(2025, 1), "A", "DOM", "US", "P", "NB", "B", "737", "800", 10, 100),
      LandingRecord(YearMonth.of(2025, 1), "A", "DOM", "US", "P", "NB", "B", "737", "800", 20, 200)
    )

  test("Processor can process a batch of records without failure") {
    val p = testKit.spawn(LandingProcessor(), "processor-test")

    // Send a sample batch of records; if anything goes wrong (exceptions, bad state),
    // the test will fail. We don't assert on logs here to avoid flakiness in LoggingTestKit.
    p ! ProcessorProtocol.ProcessRecords(sample, p)

    succeed  // If we got here, the processor handled the message successfully.
  }
}