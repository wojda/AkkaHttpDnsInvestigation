package org.danielwojda.investigation

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest._
import org.scalatest.concurrent.Eventually

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class AkkaHttpDnsSpec extends FlatSpec with Matchers with GivenWhenThen with Eventually with BeforeAndAfterAll {
  private implicit val actorSystem = ActorSystem("test")
  private implicit val mat = ActorMaterializer()
  implicit override val patienceConfig = PatienceConfig(timeout = 10 seconds)

  private val serverV1 = Docker.startServer(name = "server_v1", hostPort = 8081)
  private val serverV2 = Docker.startServer(name = "server_v2", hostPort = 8082)
  private val akkaClient = Docker.startAkkaClient(downstreamDependencyIpAddress = serverV1.ipAddress)


  "Akka Http Client" should "honour dns positive ttl" in {
    Given("server_v1, server_v2 and akka-client are up and running")
    And("akka-client is calling server_v1")
    eventually {
      Await.result(serverV1.receivedRequests(), 5 seconds) should be > 0L
      Await.result(serverV2.receivedRequests(), 5 seconds) shouldBe 0L
    }

    When("DNS entry has been changed and resolves to server_v2 IP")
    Docker.overrideExistingHostsEntry(akkaClient, "server.com", serverV2.ipAddress)

    Then("akka-client is calling server_v2")
    eventually {
      Await.result(serverV2.receivedRequests(), 5 seconds) should be > 0L
    }
  }

  override protected def afterAll(): Unit = {
    Docker.destroy(akkaClient)
    Docker.destroy(serverV2)
    Docker.destroy(serverV1)
  }
}
