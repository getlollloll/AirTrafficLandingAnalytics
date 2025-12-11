package air.messages

import air.service.ForecastService.AirlineForecast

object WriterProtocol {
  sealed trait Command

  final case class WriteForecasts(path: String, forecasts: Seq[AirlineForecast])
    extends Command
}