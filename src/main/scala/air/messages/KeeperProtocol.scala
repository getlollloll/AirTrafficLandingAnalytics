package air.messages

import air.service.ForecastService.AirlineForecast
import air.service.BaselineCalculator.MonthlyAggregate

object KeeperProtocol {
  sealed trait Command

  /** Anomaly at (period, airline). */
  final case class AnomalyDetected(
                                    current: MonthlyAggregate,
                                    forecast: AirlineForecast,
                                    deviation: Double
                                  ) extends Command

  case object PrintSummary extends Command
}