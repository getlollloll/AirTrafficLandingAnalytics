package air.messages

import akka.actor.typed.ActorRef
import air.model.LandingRecord

object FetcherProtocol {
  sealed trait Command

  final case class StartFetch(replyTo: ActorRef[Command]) extends Command
  final case class RecordsFetched(records: Seq[LandingRecord]) extends Command
}