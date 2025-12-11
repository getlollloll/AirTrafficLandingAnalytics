package air

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LoggingTestKit}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import air.actors.LandingFetcher
import air.messages.FetcherProtocol

class LandingFetcherSpec extends AnyFunSuite with BeforeAndAfterAll {

  private val testKit = ActorTestKit()
  implicit private val system: akka.actor.typed.ActorSystem[Nothing] = testKit.system

  override def afterAll(): Unit = testKit.shutdownTestKit()

  test("Fetcher reads CSV and logs record count") {
    val tmp = Files.createTempFile("fetcher-test", ".csv")
    val rows =
      "Activity Period,Operating Airline,GEO Summary,GEO Region,Landing Aircraft Type,Aircraft Body Type,Aircraft Manufacturer,Aircraft Model,Aircraft Version,Landing Count,Total Landed Weight\n" +
        "202501,AIR,DOM,US,P,NB,B,737,800,10,100.0\n" +
        "202502,AIR,DOM,US,P,NB,B,737,800,20,200.0\n"

    Files.write(tmp, rows.getBytes(StandardCharsets.UTF_8))

    val fetcher = testKit.spawn(LandingFetcher(tmp.toString), "fetcher-test")
    val dummy   = testKit.createTestProbe[FetcherProtocol.Command]()

    LoggingTestKit
      .info("Read 2 records.")
      .expect {
        fetcher ! FetcherProtocol.StartFetch(dummy.ref)
      }
  }
}