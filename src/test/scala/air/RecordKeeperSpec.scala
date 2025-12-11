package air

import air.actors.RecordKeeper
import air.messages.KeeperProtocol
import air.service.BaselineCalculator.MonthlyAggregate
import air.service.ForecastService.AirlineForecast
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LoggingTestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

import java.time.YearMonth

class RecordKeeperSpec extends AnyFunSuite with BeforeAndAfterAll {

  private val testKit = ActorTestKit()
  implicit private val system: akka.actor.typed.ActorSystem[Nothing] = testKit.system

  override def afterAll(): Unit = testKit.shutdownTestKit()

  test("RecordKeeper stores anomaly and logs it") {
    val keeper = testKit.spawn(RecordKeeper(), "keeper-test")

    val current  = MonthlyAggregate(YearMonth.of(2025, 11), "A", 100, 2000)
    val forecast = AirlineForecast("A", YearMonth.of(2025, 12), 50, 1500)

    LoggingTestKit
      .warn("ANOMALY")
      .expect {
        keeper ! KeeperProtocol.AnomalyDetected(current, forecast, 0.5)
        keeper ! KeeperProtocol.PrintSummary
      }
  }

  test("RecordKeeper can record multiple anomalies and still print summary") {
    val keeper = testKit.spawn(RecordKeeper(), "keeper-multi")

    val current1  = MonthlyAggregate(YearMonth.of(2025, 11), "A", 100, 2000)
    val forecast1 = AirlineForecast("A", YearMonth.of(2025, 12), 50, 1500)

    val current2  = MonthlyAggregate(YearMonth.of(2025, 11), "B", 200, 3000)
    val forecast2 = AirlineForecast("B", YearMonth.of(2025, 12), 80, 2500)

    LoggingTestKit
      .warn("ANOMALY")
      .withOccurrences(2)
      .expect {
        keeper ! KeeperProtocol.AnomalyDetected(current1, forecast1, 0.5)
        keeper ! KeeperProtocol.AnomalyDetected(current2, forecast2, 0.4)
        keeper ! KeeperProtocol.PrintSummary
      }
  }
}