package air.formats

import java.time.YearMonth

object Formats {

  /** activity_period is like 202511 (YYYYMM), but may contain quotes or decimals. */
  def parseActivityPeriod(s: String): Option[YearMonth] = {
    if (s == null) return None
    val digits = s.filter(_.isDigit) // keep only 0–9
    if (digits.length < 6) None
    else {
      val v = digits.take(6) // YYYYMM
      try {
        val year  = v.substring(0, 4).toInt
        val month = v.substring(4, 6).toInt
        Some(YearMonth.of(year, month))
      }
      catch {
        case _: Throwable => None
      }
    }
  }

  def cleanNumber(s: String): String =
    if (s == null) ""
    else s.replace("\"", "").replace(",", "").trim

  def parseLong(s: String): Option[Long] = {
    val cleaned = cleanNumber(s)
    if (cleaned.isEmpty) None
    else
      try Some(cleaned.toLong)
      catch { case _: Throwable => None }
  }

  def parseDouble(s: String): Option[Double] = {
    val cleaned = cleanNumber(s)
    if (cleaned.isEmpty) None
    else
      try Some(cleaned.toDouble)
      catch { case _: Throwable => None }
  }
}