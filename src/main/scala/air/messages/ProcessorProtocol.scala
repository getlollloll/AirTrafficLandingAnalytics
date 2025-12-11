package air.messages

import akka.actor.typed.ActorRef
import air.model.LandingRecord
import air.service.BaselineCalculator.MonthlyAggregate

object ProcessorProtocol {
  sealed trait Command

  final case class ProcessRecords(
                                   records: Seq[LandingRecord],
                                   replyTo: ActorRef[Command]
                                 ) extends Command

  final case class AggregatesComputed(
                                       monthly: Seq[MonthlyAggregate]
                                     ) extends Command
}