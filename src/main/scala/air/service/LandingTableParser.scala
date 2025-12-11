package air.service

import com.phasmidsoftware.tableparser.core.parse.{CellParser, TableParserHelper}
import com.phasmidsoftware.tableparser.core.table.Table

import java.io.File
import scala.io.Codec
import scala.util.{Failure, Success}

/**
 * Row representation for the SFO Air Traffic Landings CSV.
 * The field names must match the column names in the CSV header.
 */
case class LandingRow(
                       activity_period: String,
                       operating_airline: String,
                       geo_summary: String,
                       geo_region: String,
                       landing_aircraft_type: String,
                       aircraft_body_type: String,
                       aircraft_manufacturer: String,
                       aircraft_model: String,
                       aircraft_version: String,
                       landing_count: String,
                       total_landed_weight: String
                     )

/**
 * Companion object for LandingRow:
 *  - Extends TableParserHelper, which:
 *      * defines an implicit CellParser[LandingRow]
 *      * defines an implicit TableParser[Table[LandingRow]]
 *    so that Table.parseFile can work.
 */
object LandingRow extends TableParserHelper[LandingRow]() {

  /**
   * Define how to parse one CSV row into a LandingRow.
   *
   * We use cellParser11 because LandingRow has 11 fields.
   * The `fields` sequence MUST match the header names in the CSV exactly.
   */
  override def cellParser: CellParser[LandingRow] =
    cellParser11[String, String, String, String, String,
      String, String, String, String, String, String, LandingRow](
      LandingRow,                           // constructor
      Seq(
        "Activity Period",       // activity_period
        "Operating Airline",     // operating_airline
        "GEO Summary",           // geo_summary
        "GEO Region",            // geo_region
        "Landing Aircraft Type", // landing_aircraft_type
        "Aircraft Body Type",    // aircraft_body_type
        "Aircraft Manufacturer", // aircraft_manufacturer
        "Aircraft Model",        // aircraft_model
        "Aircraft Version",      // aircraft_version
        "Landing Count",         // landing_count
        "Total Landed Weight"    // total_landed_weight
      )
    )
}

/**
 * LandingTableParser:
 *  - Uses com.phasmidsoftware.tableparser.core.table.Table
 *    and your professor's TableParser helpers
 *  - Reads a CSV file into a typed Table[LandingRow]
 *  - Returns the rows as a Seq[LandingRow].
 */
object LandingTableParser {

  def readRows(path: String): Seq[LandingRow] = {
    implicit val codec: Codec = Codec.UTF8

    // T is Table[LandingRow]; implicit TableParser[Table[LandingRow]]
    // is provided by LandingRow (via TableParserHelper).
    Table.parseFile[Table[LandingRow]](new File(path)) match {
      case Success(table) =>
        table.toSeq
      case Failure(e) =>
        throw new RuntimeException(
          s"Failed to parse landings CSV at $path: ${e.getMessage}",
          e
        )
    }
  }
}