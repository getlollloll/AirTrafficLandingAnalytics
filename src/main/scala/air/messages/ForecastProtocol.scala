package air.messages

import air.service.BaselineCalculator.MonthlyAggregate
import air.service.ForecastService.AirlineForecast

object ForecastProtocol {
  sealed trait Command

  final case class ComputeForecasts(monthly: Seq[MonthlyAggregate]) extends Command
  final case class ForecastsReady(forecasts: Seq[AirlineForecast])   extends Command
}