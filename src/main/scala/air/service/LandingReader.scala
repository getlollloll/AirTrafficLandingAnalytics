package air.service

import air.formats.Formats
import air.model.LandingRecord

object LandingReader {

  def read(csvPath: String): Seq[LandingRecord] = {
    val rows: Seq[LandingRow] = LandingTableParser.readRows(csvPath)
    rows.flatMap(toLandingRecord)
  }

  private def toLandingRecord(r: LandingRow): Option[LandingRecord] = {
    import Formats._

    val periodOpt       = parseActivityPeriod(r.activity_period)
    val landingCountOpt = parseLong(r.landing_count)
    val weightOpt       = parseDouble(r.total_landed_weight)

    for {
      p  <- periodOpt
      lc <- landingCountOpt
      w  <- weightOpt
    } yield LandingRecord(
      period       = p,
      airline      = r.operating_airline,
      geoSummary   = r.geo_summary,
      geoRegion    = r.geo_region,
      landingType  = r.landing_aircraft_type,
      aircraftBody = r.aircraft_body_type,
      manufacturer = r.aircraft_manufacturer,
      model        = r.aircraft_model,
      version      = r.aircraft_version,
      landings     = lc,
      landedWeight = w
    )
  }
}