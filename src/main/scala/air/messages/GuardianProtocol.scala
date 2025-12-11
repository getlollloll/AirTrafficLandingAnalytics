package air.messages

object GuardianProtocol {
  sealed trait Command

  /** Kick off the whole pipeline. */
  case object Start extends Command
}