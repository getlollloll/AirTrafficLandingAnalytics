package air.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import air.messages.ForecastProtocol._
import air.messages.KeeperProtocol
import air.service.BaselineCalculator.MonthlyAggregate
import air.service.{BaselineCalculator, ForecastService}
import air.service.ForecastService.AirlineForecast

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

object LandingRecommender {

  // deviation threshold for anomaly (e.g. 30%)
  private val DeviationThreshold = 0.30

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>
      val keeper = ctx.spawn(RecordKeeper(), "record-keeper")

      Behaviors.receiveMessage {
        case ComputeForecasts(monthly: Seq[MonthlyAggregate]) =>
          // -----------------------------
          // 1) One-step-ahead forecasts: used ONLY for anomaly detection
          // -----------------------------
          val oneStepForecasts: Seq[AirlineForecast] =
            ForecastService.forecastNextMonthPerAirline(monthly, window = 6)

          ctx.log.info(
            s"Computed ${oneStepForecasts.size} airline one-step forecasts for anomaly detection."
          )

          val byAirline =
            monthly.groupBy(_.airline).view.mapValues(_.sortBy(_.period)).toMap

          oneStepForecasts.foreach { f =>
            byAirline.get(f.airline).flatMap(_.lastOption).foreach { last =>
              val actual    = last.landingCount.toDouble
              val predicted = f.predictedLandingCount
              if (actual > 0) {
                val deviation = math.abs(actual - predicted) / actual
                if (deviation > DeviationThreshold) {
                  keeper ! KeeperProtocol.AnomalyDetected(last, f, deviation)
                }
              }
            }
          }

          keeper ! KeeperProtocol.PrintSummary

          // -----------------------------
          // 2) 6-month GLOBAL forecasts:  used for CSV output (future)
          // -----------------------------
          val futureForecasts: Seq[AirlineForecast] =
            ForecastService.forecastNext6MonthsGlobal(monthly, window = 6)

          ctx.log.info(
            s"Computed ${futureForecasts.size} airline-month future forecasts (6-month horizon)."
          )

          writeForecastsCsv("output/forecasts.csv", futureForecasts, msg => ctx.log.info(msg))

          Behaviors.same

        case ForecastsReady(_) =>
          Behaviors.same
      }
    }

  /**
   * Helper to write airline-level forecasts to a CSV file.
   */
  private def writeForecastsCsv(
                                 path: String,
                                 forecasts: Seq[AirlineForecast],
                                 log: String => Unit
                               ): Unit = {
    val outputPath = Paths.get(path)
    val parent = outputPath.getParent
    if (parent != null && !Files.exists(parent)) {
      Files.createDirectories(parent)
    }

    val header = "airline,next_period,predicted_landings,predicted_weight_lb"

    val lines = forecasts.map { f =>
      val airline = f.airline.replace("\"", "\"\"")
      val period  = f.nextPeriod.toString
      val landings = f.predictedLandingCount
      val weight   = f.predictedTotalWeight

      s""""$airline","$period",$landings,$weight"""
    }

    val content = (header +: lines).mkString("\n") + "\n"

    Files.write(
      outputPath,
      content.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING
    )

    log(s"[CSV] Wrote ${forecasts.size} forecasts to $path")
  }
}