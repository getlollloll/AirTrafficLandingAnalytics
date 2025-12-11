package air.model

import java.time.YearMonth

final case class LandingRecord(
  period:        YearMonth,
  airline:       String,
  geoSummary:    String,
  geoRegion:     String,
  landingType:   String,
  aircraftBody:  String,
  manufacturer:  String,
  model:         String,
  version:       String,
  landings:      Long,
  landedWeight:  Double
)