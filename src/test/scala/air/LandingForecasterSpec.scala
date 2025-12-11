package air

import air.service.BaselineCalculator.MonthlyAggregate
import air.service.ForecastService
import org.scalatest.funsuite.AnyFunSuite

import java.time.YearMonth

class LandingForecasterSpec extends AnyFunSuite {

  test("ForecastService produces predictions for each airline using moving average") {

    val months = Seq(
      MonthlyAggregate(YearMonth.of(2025, 1), "A1", 10, 100),
      MonthlyAggregate(YearMonth.of(2025, 2), "A1", 20, 200),
      MonthlyAggregate(YearMonth.of(2025, 3), "A1", 30, 300),
      MonthlyAggregate(YearMonth.of(2025, 1), "A2", 5,  50),
      MonthlyAggregate(YearMonth.of(2025, 2), "A2", 15, 150),
      MonthlyAggregate(YearMonth.of(2025, 3), "A2", 25, 250)
    )

    // This matches your implementation:
    // def forecastNextMonthPerAirline(monthly: Seq[MonthlyAggregate], window: Int)
    val forecasts = ForecastService.forecastNextMonthPerAirline(months, window = 2)

    // Should produce at least one forecast per airline
    assert(forecasts.nonEmpty)
    assert(forecasts.map(_.airline).toSet == Set("A1", "A2"))

    // Check moving-average math for A1 (last 2 months: 20, 30 → avg 25)
    val a1 = forecasts.find(_.airline == "A1").get
    assert(math.abs(a1.predictedLandingCount - 25.0) < 1e-6)

    // And for A2 (last 2 months: 15, 25 → avg 20)
    val a2 = forecasts.find(_.airline == "A2").get
    assert(math.abs(a2.predictedLandingCount - 20.0) < 1e-6)
  }

  test("ForecastService returns empty list when there are no aggregates") {
    val forecasts =
      ForecastService.forecastNextMonthPerAirline(Seq.empty, window = 3)

    assert(forecasts.isEmpty)
  }

  test("ForecastService uses only the last N months when computing moving average") {
    val airline = "A3"
    val months = Seq(
      MonthlyAggregate(YearMonth.of(2025, 1), airline, 10, 100),
      MonthlyAggregate(YearMonth.of(2025, 2), airline, 20, 200),
      MonthlyAggregate(YearMonth.of(2025, 3), airline, 30, 300),
      MonthlyAggregate(YearMonth.of(2025, 4), airline, 40, 400)
    )

    val forecasts =
      ForecastService.forecastNextMonthPerAirline(months, window = 2)

    assert(forecasts.nonEmpty)

    val f = forecasts.find(_.airline == airline).get
    // last 2 months: 30, 40 => average 35
    assert(math.abs(f.predictedLandingCount - 35.0) < 1e-6)
    assert(f.nextPeriod == YearMonth.of(2025, 5))
  }
}