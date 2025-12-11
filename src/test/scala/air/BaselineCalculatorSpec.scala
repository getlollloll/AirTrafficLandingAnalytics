package air

import air.model.LandingRecord
import air.service.BaselineCalculator
import air.service.BaselineCalculator.MonthlyAggregate
import org.scalatest.funsuite.AnyFunSuite

import java.time.YearMonth

class BaselineCalculatorSpec extends AnyFunSuite {

  test("monthlyAggregates should group by (period, airline)") {
    val p = YearMonth.of(2025, 1)

    val r1 = LandingRecord(
      period        = p,
      airline       = "AIR1",
      geoSummary    = "DOMESTIC",
      geoRegion     = "US",
      landingType   = "PASSENGER",
      aircraftBody  = "NARROW",
      manufacturer  = "BOEING",
      model         = "737",
      version       = "800",
      landings      = 10L,
      landedWeight  = 1000.0
    )

    val r2 = r1.copy(landings = 15L)                 // same airline, same month
    val r3 = r1.copy(airline = "AIR2", landings = 5) // different airline

    val aggs = BaselineCalculator.monthlyAggregates(Seq(r1, r2, r3))

    assert(aggs.size == 2)

    val air1 = aggs.find(a => a.airline == "AIR1").get
    assert(air1.landingCount == 25L)
    assert(air1.totalLandedWeight == 2000.0)

    val air2 = aggs.find(a => a.airline == "AIR2").get
    assert(air2.landingCount == 5L)
  }

  test("movingAverageBaseline + baselineAccuracy should give high accuracy on smooth data") {
    val airline = "AIR1"
    val months  = (1 to 8).map { m =>
      MonthlyAggregate(
        period            = YearMonth.of(2025, m),
        airline           = airline,
        landingCount      = m * 10L, // steadily increasing
        totalLandedWeight = 0.0
      )
    }

    val baselines = BaselineCalculator.movingAverageBaseline(months, window = 3)
    val acc       = BaselineCalculator.baselineAccuracy(baselines)

    assert(acc > 50.0) // should be a pretty good fit
  }

  test("monthlyAggregates should sum landingCount and weight for duplicate rows") {
    val p = YearMonth.of(2025, 1)

    val r1 = LandingRecord(
      period        = p,
      airline       = "AIR",
      geoSummary    = "DOMESTIC",
      geoRegion     = "US",
      landingType   = "PASSENGER",
      aircraftBody  = "NARROW",
      manufacturer  = "BOEING",
      model         = "737",
      version       = "800",
      landings      = 10L,
      landedWeight  = 1000.0
    )

    val r2 = r1.copy(landings = 5L, landedWeight = 500.0)

    val aggregates = BaselineCalculator.monthlyAggregates(Seq(r1, r2))

    assert(aggregates.size == 1)
    val agg = aggregates.head
    assert(agg.landingCount == 15L)
    assert(agg.totalLandedWeight == 1500.0)
  }

  test("monthlyAggregates should be empty when given no records") {
    val aggregates = BaselineCalculator.monthlyAggregates(Seq.empty)
    assert(aggregates.isEmpty)
  }
}