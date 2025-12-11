package air.service

import air.service.BaselineCalculator.MonthlyAggregate
import java.time.YearMonth

object ForecastService {

  /** Prediction for next month per airline. */
  final case class AirlineForecast(
                                    airline:               String,
                                    nextPeriod:            YearMonth,
                                    predictedLandingCount: Double,
                                    predictedTotalWeight:  Double
                                  )

  /**
   * Forecast next-month landings + weight per airline using simple moving average (window months).
   * (Used for anomaly detection.)
   */
  def forecastNextMonthPerAirline(
                                   monthly: Seq[MonthlyAggregate],
                                   window:  Int
                                 ): Seq[AirlineForecast] = {
    monthly
      .groupBy(_.airline)
      .toSeq
      .flatMap { case (airline, rows) =>
        if (rows.isEmpty) None
        else {
          val sorted  = rows.sortBy(_.period)
          val lastN   = sorted.takeRight(window)
          val last    = sorted.last
          val nextPer = last.period.plusMonths(1)

          val avgLanding =
            lastN.map(_.landingCount.toDouble).sum / lastN.size.toDouble
          val avgWeight =
            lastN.map(_.totalLandedWeight.toDouble).sum / lastN.size.toDouble

          Some(
            AirlineForecast(
              airline               = airline,
              nextPeriod            = nextPer,
              predictedLandingCount = avgLanding,
              predictedTotalWeight  = avgWeight
            )
          )
        }
      }
  }

  /**
   * NEW: Forecast the next 6 calendar months for every airline,
   * starting from the GLOBAL last period in the dataset.
   *
   * Uses a simple trend model:
   *   - compute average monthly change (delta) over the last `window` points
   *   - then walk forward 6 months: lastActual + k * delta
   */
  def forecastNext6MonthsGlobal(
                                 monthly: Seq[MonthlyAggregate],
                                 window:  Int
                               ): Seq[AirlineForecast] = {

    if (monthly.isEmpty) return Seq.empty

    // 1) Latest month in the WHOLE dataset (from your CSV)
    val lastPeriod: YearMonth = monthly.map(_.period).max

    // 2) Next 6 months after that
    val start: YearMonth = lastPeriod.plusMonths(1)
    val forecastPeriods: Seq[YearMonth] =
      (0 until 6).map(i => start.plusMonths(i))

    // 3) History grouped by airline
    val byAirline: Map[String, Seq[MonthlyAggregate]] =
      monthly.groupBy(_.airline)

    val forecastsPerAirline: Seq[Seq[AirlineForecast]] =
      for {
        (airline, series) <- byAirline.toSeq
        sorted            = series.sortBy(_.period)
        recent            = sorted.takeRight(window)
        if recent.size >= 2  // need at least 2 points to compute a delta
      } yield {
        // ---- average monthly change (delta) in landings ----
        val landingDeltas: Seq[Double] =
          recent.sliding(2).collect {
            case Seq(prev, curr) =>
              curr.landingCount.toDouble - prev.landingCount.toDouble
          }.toSeq

        val avgLandingDelta: Double =
          landingDeltas.sum / landingDeltas.size.toDouble

        // ---- average monthly change (delta) in weight ----
        val weightDeltas: Seq[Double] =
          recent.sliding(2).collect {
            case Seq(prev, curr) =>
              curr.totalLandedWeight.toDouble - prev.totalLandedWeight.toDouble
          }.toSeq

        val avgWeightDelta: Double =
          weightDeltas.sum / weightDeltas.size.toDouble

        // Start from the LAST ACTUAL value
        var lastLanding: Double = recent.last.landingCount.toDouble
        var lastWeight:  Double = recent.last.totalLandedWeight.toDouble

        forecastPeriods.map { p =>
          // advance by one step
          lastLanding = math.max(0.0, lastLanding + avgLandingDelta)
          lastWeight  = math.max(0.0, lastWeight + avgWeightDelta)

          AirlineForecast(
            airline               = airline,
            nextPeriod            = p,
            predictedLandingCount = lastLanding,
            predictedTotalWeight  = lastWeight
          )
        }
      }

    forecastsPerAirline.flatten
  }
}