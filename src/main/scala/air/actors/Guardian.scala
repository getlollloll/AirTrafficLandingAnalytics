package air.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import air.messages.GuardianProtocol
import air.messages.FetcherProtocol.StartFetch

object Guardian {

  def apply(csvPath: String): Behavior[GuardianProtocol.Command] =
    Behaviors.setup { ctx =>
      val fetcher = ctx.spawn(LandingFetcher(csvPath), "landing-fetcher")
      ctx.log.info("Guardian started.")

      Behaviors.receiveMessage {
        case GuardianProtocol.Start =>
          ctx.log.info("Guardian received Start, triggering fetcher.")
          fetcher ! StartFetch(fetcher) // self just as dummy
          Behaviors.same
      }
    }
}