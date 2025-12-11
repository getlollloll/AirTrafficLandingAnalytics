package air.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import air.messages.ProcessorProtocol._
import air.messages.ForecastProtocol
import air.model.LandingRecord
import air.service.BaselineCalculator

object LandingProcessor {

  def apply(): Behavior[Command] =
    Behaviors.setup { ctx =>
      val recommender =
        ctx.spawn(LandingRecommender(), "landing-recommender")

      Behaviors.receiveMessage {
        case ProcessRecords(records, _) =>
          ctx.log.info(s"Processing ${records.size} records into monthly aggregates.")
          val monthly = BaselineCalculator.monthlyAggregates(records)
          val baselines =
            BaselineCalculator.movingAverageBaseline(monthly, window = 6)
          val acc = BaselineCalculator.baselineAccuracy(baselines)
          ctx.log.info(f"Baseline trend accuracy: $acc%.2f%%")

          recommender ! ForecastProtocol.ComputeForecasts(monthly)
          Behaviors.same

        case AggregatesComputed(_) =>
          Behaviors.same
      }
    }
}