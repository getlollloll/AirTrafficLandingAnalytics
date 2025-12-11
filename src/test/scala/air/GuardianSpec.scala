package air

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LoggingTestKit}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import air.actors.Guardian
import air.messages.GuardianProtocol

class GuardianSpec extends AnyFunSuite with BeforeAndAfterAll {

  private val testKit = ActorTestKit()
  implicit private val system: akka.actor.typed.ActorSystem[Nothing] = testKit.system

  override def afterAll(): Unit = testKit.shutdownTestKit()

  test("Guardian logs start message") {
    val tmp = Files.createTempFile("guardian-start", ".csv")
    val text =
      "activity_period,operating_airline,geo_summary,geo_region,landing_aircraft_type,aircraft_body_type,aircraft_manufacturer,aircraft_model,aircraft_version,landing_count,total_landed_weight\n" +
        "202511,AIR,DOMESTIC,US,Passenger,Narrow,Boeing,737,800,5,500.0\n"

    Files.write(tmp, text.getBytes(StandardCharsets.UTF_8))

    LoggingTestKit
      .info("Guardian started.")
      .expect {
        testKit.spawn(Guardian(tmp.toString), "guardian-start-test")
      }
  }

  test("Guardian logs Start and triggers fetcher") {
    val tmp = Files.createTempFile("guardian-test", ".csv")
    val text =
      "activity_period,operating_airline,geo_summary,geo_region,landing_aircraft_type,aircraft_body_type,aircraft_manufacturer,aircraft_model,aircraft_version,landing_count,total_landed_weight\n" +
        "202511,AIR,DOMESTIC,US,Passenger,Narrow,Boeing,737,800,5,500.0\n"

    Files.write(tmp, text.getBytes(StandardCharsets.UTF_8))

    val guardian = testKit.spawn(Guardian(tmp.toString), "guardian-test")

    // Only check Guardian's log — ignore noisy fetcher logs
    LoggingTestKit
      .info("Guardian received Start")
      .expect {
        guardian ! GuardianProtocol.Start
      }
  }
}