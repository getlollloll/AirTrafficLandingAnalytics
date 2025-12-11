package air

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LoggingTestKit}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll

import java.time.YearMonth

import air.actors.CsvWriter
import air.messages.WriterProtocol
import air.service.ForecastService.AirlineForecast

class CsvWriterSpec extends AnyFunSuite with BeforeAndAfterAll {

  private val testKit = ActorTestKit()
  implicit private val system: akka.actor.typed.ActorSystem[Nothing] = testKit.system

  override def afterAll(): Unit = testKit.shutdownTestKit()

  test("CsvWriter logs number of forecasts and target path") {
    val writer = testKit.spawn(CsvWriter(), "csv-writer-test")

    val forecasts = Seq(
      AirlineForecast("AIR1", YearMonth.of(2025, 12), 100.0, 3000.0),
      AirlineForecast("AIR2", YearMonth.of(2025, 12), 50.0, 1200.0)
    )

    val outPath = "/tmp/air-forecasts-test.csv"

    LoggingTestKit
      .info("[CSV] Wrote 2 forecasts to")
      .expect {
        writer ! WriterProtocol.WriteForecasts(outPath, forecasts)
      }
  }

  test("CsvWriter logs correctly when there are zero forecasts") {
    val writer = testKit.spawn(CsvWriter(), "csv-writer-empty")

    val forecasts = Seq.empty[AirlineForecast]
    val outPath   = "/tmp/air-forecasts-empty.csv"

    LoggingTestKit
      .info("[CSV] Wrote 0 forecasts to")
      .expect {
        writer ! WriterProtocol.WriteForecasts(outPath, forecasts)
      }
  }
}