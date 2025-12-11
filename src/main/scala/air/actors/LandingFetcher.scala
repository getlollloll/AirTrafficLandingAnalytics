package air.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import air.messages.FetcherProtocol._
import air.messages.ProcessorProtocol
import air.service.LandingReader

object LandingFetcher {

  def apply(csvPath: String): Behavior[Command] =
    Behaviors.setup { ctx =>
      val processor =
        ctx.spawn(LandingProcessor(), "landing-processor")

      Behaviors.receiveMessage {
        case StartFetch(_) =>
          ctx.log.info(s"Reading CSV from: $csvPath")
          val records = LandingReader.read(csvPath)
          ctx.log.info(s"Read ${records.size} records.")
          processor ! ProcessorProtocol.ProcessRecords(records, processor)
          Behaviors.same

        case RecordsFetched(_) =>
          Behaviors.same
      }
    }
}