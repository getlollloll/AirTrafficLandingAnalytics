package air.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import air.messages.KeeperProtocol
import air.service.ForecastService.AirlineForecast
import air.service.BaselineCalculator.MonthlyAggregate

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

object RecordKeeper {

  def apply(): Behavior[KeeperProtocol.Command] =
    Behaviors.setup { ctx =>
      // (current, forecast, deviationFraction)
      var anomalies: List[(MonthlyAggregate, AirlineForecast, Double)] = Nil

      Behaviors.receiveMessage {
        case KeeperProtocol.AnomalyDetected(current, forecast, deviation) =>
          // deviation is stored as a fraction (e.g., 0.5 = 50%)
          anomalies ::= (current, forecast, deviation)
          Behaviors.same

        case KeeperProtocol.PrintSummary =>
          // 1) Log summary + individual anomalies (what you already had)
          ctx.log.info(s"Total anomalies detected: ${anomalies.size}")
          anomalies.foreach { case (curr, forecast, dev) =>
            ctx.log.warn(
              f"[ANOMALY] Airline=${curr.airline}, period=${curr.period}, " +
                f"actual=${curr.landingCount}, " +
                f"predicted=${forecast.predictedLandingCount}%.2f, " +
                f"deviation=${dev * 100}%.2f%%"
            )
          }

          // 2) ALSO write anomalies to CSV file: output/anomalies.csv
          val outputPath = Paths.get("output/anomalies.csv")
          val parent = outputPath.getParent
          if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
          }

          val header = "airline,period,actual,predicted,deviation_percent"

          // reverse so anomalies are in the order they were detected (optional)
          val lines = anomalies.reverse.map { case (curr, forecast, dev) =>
            val airline = curr.airline.replace("\"", "\"\"")
            val period = curr.period.toString
            val actual = curr.landingCount
            val predicted = forecast.predictedLandingCount
            val deviationPercent = dev * 100

            s""""$airline","$period",$actual,$predicted,$deviationPercent"""
          }

          val content = (header +: lines).mkString("\n") + "\n"

          Files.write(
            outputPath,
            content.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
          )

          ctx.log.info(s"[CSV] Wrote ${anomalies.size} anomalies to output/anomalies.csv")

          Behaviors.same
      }
    }
}