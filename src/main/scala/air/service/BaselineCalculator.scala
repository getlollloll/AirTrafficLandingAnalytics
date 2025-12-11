package air.service

import air.model.LandingRecord
import java.time.YearMonth

object BaselineCalculator {

  /** Monthly aggregate per (period, airline). */
  final case class MonthlyAggregate(
                                     period:           YearMonth,
                                     airline:          String,
                                     landingCount:     Long,
                                     totalLandedWeight: Double
                                   )

  def monthlyAggregates(records: Seq[LandingRecord]): Seq[MonthlyAggregate] =
    records
      .groupBy(r => (r.period, r.airline))    // <- use .airline
      .toSeq
      .map { case ((period, airline), rows) =>
        MonthlyAggregate(
          period,
          airline,
          rows.map(_.landings).sum,          // <- use .landings
          rows.map(_.landedWeight).sum       // <- use .landedWeight
        )
      }
      .sortBy(r => (r.airline, r.period))

  def movingAverageBaseline(
                             monthly: Seq[MonthlyAggregate],
                             window:  Int
                           ): Seq[(MonthlyAggregate, Double)] =
    monthly
      .groupBy(_.airline)
      .toSeq
      .flatMap { case (_, rows) =>
        val sorted = rows.sortBy(_.period)
        sorted.indices.map { idx =>
          val history = sorted.slice((idx - window).max(0), idx)
          val baseline =
            if (history.isEmpty) sorted(idx).landingCount.toDouble
            else history.map(_.landingCount.toDouble).sum / history.size.toDouble
          (sorted(idx), baseline)            // <- use sorted(idx), not sorted[idx]
        }
      }

  /** Baseline trend accuracy: 100 - mean(|actual - baseline| / actual)*100 */
  def baselineAccuracy(baselines: Seq[(MonthlyAggregate, Double)]): Double = {
    if (baselines.isEmpty) 0.0
    else {
      val errors = baselines.collect {
        case (agg, base) if agg.landingCount > 0 =>
          math.abs(agg.landingCount.toDouble - base) / agg.landingCount.toDouble
      }
      if (errors.isEmpty) 0.0
      else 100.0 - (errors.sum / errors.size * 100.0)
    }
  }

  /** Forecast next-month landing count per airline (simple moving-average). */
  def forecastNextMonthPerAirline(
                                   aggregates: Seq[MonthlyAggregate],
                                   window:     Int = 6
                                 ): Seq[(String, YearMonth, Double)] =
    aggregates
      .groupBy(_.airline)
      .toSeq
      .flatMap { case (airline, rows) =>
        if (rows.isEmpty) None
        else {
          val sorted   = rows.sortBy(_.period)
          val recent   = sorted.takeRight(window)
          val denom    = math.max(recent.size, 1)
          val avg      = recent.map(_.landingCount.toDouble).sum / denom
          val next     = sorted.last.period.plusMonths(1)
          Some((airline, next, avg))
        }
      }

  /** Forecast next-month landed weight per airline. */
  def forecastNextMonthWeightPerAirline(
                                         aggregates: Seq[MonthlyAggregate],
                                         window:     Int = 6
                                       ): Seq[(String, YearMonth, Double)] =
    aggregates
      .groupBy(_.airline)
      .toSeq
      .flatMap { case (airline, rows) =>
        if (rows.isEmpty) None
        else {
          val sorted   = rows.sortBy(_.period)
          val recent   = sorted.takeRight(window)
          val denom    = math.max(recent.size, 1)
          val avgW     = recent.map(_.totalLandedWeight).sum / denom
          val next     = sorted.last.period.plusMonths(1)
          Some((airline, next, avgW))
        }
      }
}