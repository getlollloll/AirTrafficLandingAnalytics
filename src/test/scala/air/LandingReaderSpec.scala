package air

import air.model.LandingRecord
import air.service.LandingReader
import org.scalatest.funsuite.AnyFunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.YearMonth

class LandingReaderSpec extends AnyFunSuite {

  /** Helper to create a temporary CSV file with the given content. */
  private def withTempCsv(content: String)(f: Path => Unit): Unit = {
    val tmp = Files.createTempFile("landing-reader-test", ".csv")
    Files.write(tmp, content.getBytes(StandardCharsets.UTF_8))
    try {
      f(tmp)
    } finally {
      Files.deleteIfExists(tmp)
    }
  }

  test("read should parse a valid CSV row into LandingRecord") {
    val csv =
      """activity_period,operating_airline,geo_summary,geo_region,landing_aircraft_type,aircraft_body_type,aircraft_manufacturer,aircraft_model,aircraft_version,landing_count,total_landed_weight
        |202511,TEST AIR,DOMESTIC,US,PASSENGER,NARROW,BOEING,737,800,123,4567.89
        |""".stripMargin

    withTempCsv(csv) { path =>
      val records: Seq[LandingRecord] = LandingReader.read(path.toString)
      assert(records.size == 1)

      val r = records.head
      assert(r.period == YearMonth.of(2025, 11))
      assert(r.airline == "TEST AIR")
      assert(r.landings == 123L)
      assert(math.abs(r.landedWeight - 4567.89) < 1e-6)
    }
  }

  test("read should skip rows where activity_period is missing") {
    val csv =
      """activity_period,operating_airline,geo_summary,geo_region,landing_aircraft_type,aircraft_body_type,aircraft_manufacturer,aircraft_model,aircraft_version,landing_count,total_landed_weight
        |,TEST AIR,DOMESTIC,US,PASSENGER,NARROW,BOEING,737,800,123,4567.89
        |""".stripMargin

    withTempCsv(csv) { path =>
      val records: Seq[LandingRecord] = LandingReader.read(path.toString)
      assert(records.isEmpty)
    }
  }

  test("read should skip rows where landing_count is not numeric") {
    val csv =
      """activity_period,operating_airline,geo_summary,geo_region,landing_aircraft_type,aircraft_body_type,aircraft_manufacturer,aircraft_model,aircraft_version,landing_count,total_landed_weight
        |202511,TEST AIR,DOMESTIC,US,PASSENGER,NARROW,BOEING,737,800,NOT_A_NUMBER,4567.89
        |""".stripMargin

    withTempCsv(csv) { path =>
      val records: Seq[LandingRecord] = LandingReader.read(path.toString)
      assert(records.isEmpty)
    }
  }
}