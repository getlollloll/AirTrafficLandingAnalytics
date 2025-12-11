package air.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import air.messages.WriterProtocol
import air.service.ForecastService.AirlineForecast

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

object CsvWriter {

  def apply(): Behavior[WriterProtocol.Command] =
    Behaviors.setup { ctx =>

      Behaviors.receiveMessage {

        // -----------------------------------------------------------
        // WRITE FORECASTS TO CSV
        // -----------------------------------------------------------
        case WriterProtocol.WriteForecasts(path, forecasts) =>
          val outputPath = Paths.get(path)

          // Ensure folder exists (e.g., "output/")
          val parent = outputPath.getParent
          if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
          }

          // CSV Header
          val header = "airline,next_period,predicted_landings,predicted_weight_lb"

          // Convert each forecast row → CSV line
          val lines = forecasts.map { f =>
            // Escape quotes inside airline names
            val airline = f.airline.replace("\"", "\"\"")
            val period = f.nextPeriod.toString
            val landings = f.predictedLandingCount
            val weight = f.predictedTotalWeight

            s""""$airline","$period",$landings,$weight"""
          }

          // Join header + rows
          val content = (header +: lines).mkString("\n") + "\n"

          // Write file (create if not exists, replace if exists)
          Files.write(
            outputPath,
            content.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
          )

          ctx.log.info(s"[CSV] Wrote ${forecasts.size} forecasts to $path")
          Behaviors.same
      }
    }
}