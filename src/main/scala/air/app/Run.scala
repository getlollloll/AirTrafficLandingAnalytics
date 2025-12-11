package air.app

import akka.actor.typed.ActorSystem
import com.typesafe.scalalogging.LazyLogging

import air.model._
import air.service._
import air.actors.Guardian
import air.messages.GuardianProtocol


object Run extends LazyLogging {

  def main(args: Array[String]): Unit = {

    // -----------------------------
    // 1) Resolve input path
    // -----------------------------
    val csvPath: String =
      if (args.nonEmpty) args(0)
      else "src/main/data/Air_Traffic_Landings_Statistics_20251118.csv"   // adjust to your actual file

    logger.info(s"[BOOT] Using CSV: $csvPath")

    // -----------------------------
    // 2) Batch analytics (slow path)
    // -----------------------------

    // 2.1 Read raw landings
    val records: Seq[LandingRecord] = LandingReader.read(csvPath)
    logger.info(s"[BATCH] Loaded ${records.size} LandingRecord rows")

    // 2.2 Monthly aggregates (period, airline)
    val monthly = BaselineCalculator.monthlyAggregates(records)
    logger.info(s"[BATCH] Computed ${monthly.size} monthly aggregates")

    // 2.3 Moving-average baseline + accuracy
    val baselines =
      BaselineCalculator.movingAverageBaseline(
        monthly,
        window = 6      // last 6 months per airline
      )

    val baselineAcc = BaselineCalculator.baselineAccuracy(baselines)
    logger.info(f"[BATCH] Baseline trend accuracy = $baselineAcc%.2f %%")

    // 2.4 Forecast next-month landings + weight per airline
    val forecasts =
      ForecastService.forecastNextMonthPerAirline(
        monthly,
        window = 6
      )

    logger.info(
      s"[BATCH] Forecasted next-month landings for ${forecasts.size} airlines"
    )

    // 2.5 Log a few sample forecasts – your “final analytics”
    forecasts
      .sortBy(_.airline)
      .take(10)
      .foreach { f =>
        logger.info(
          f"[BATCH] → ${f.airline}%-20s period=${f.nextPeriod} " +
            f"landings≈${f.predictedLandingCount}%.1f " +
            f"weight≈${f.predictedTotalWeight}%.1f lb"
        )
      }

    // -----------------------------
    // 3) Reactive pipeline (fast path)
    // -----------------------------
    //
    // Start the Akka actor system.
    // Current Guardian signature (from your code) is: Guardian(csvPath: String)
    // Deviation threshold and where to write alerts can be handled inside Guardian.
    // -----------------------------

    logger.info("[STREAM] Starting actor system (Guardian)…")

    val system: ActorSystem[GuardianProtocol.Command] =
      ActorSystem(
        Guardian(csvPath),
        "AirTrafficLandingSystem"
      )

    system ! GuardianProtocol.Start
  }
}