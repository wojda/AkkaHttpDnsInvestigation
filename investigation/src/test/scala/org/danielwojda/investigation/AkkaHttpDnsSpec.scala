package org.danielwojda.investigation

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest._
import org.scalatest.concurrent.{Eventually, ScalaFutures}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

class AkkaHttpDnsSpec extends FlatSpec with Matchers with GivenWhenThen with Eventually with BeforeAndAfterAll with ScalaFutures {
  private implicit val actorSystem = ActorSystem("test")
  private implicit val mat = ActorMaterializer()
  implicit override val patienceConfig = PatienceConfig(timeout = 10 seconds)

  "Akka Http Client" should "honour DNS positive ttl" in withDockerizedEnvironment { (akkaClient, serverV1, serverV2) =>
    Given("server_v1, server_v2 and akka-client are up and running")
    And("akka-client is calling server_v1")
    eventually { akkaClient.startSendingRequests }
    eventually {
      whenReady(serverV1.receivedRequests()) { numberOfRequests: Long =>
        numberOfRequests should be > 0L
      }
      whenReady(serverV2.receivedRequests()) { numberOfRequests: Long =>
        numberOfRequests shouldBe 0L
      }
    }

    When("DNS entry has been changed and resolves to server_v2 IP")
    Docker.overrideExistingHostsEntry(akkaClient, "server.com", serverV2.ipAddress)

    Then("akka-client is calling server_v2")
    val wait = timeout(AkkaClientConfig.akkaDnsPositiveTtl * 2)
    eventually (wait) {
      whenReady(serverV2.receivedRequests()) { numberOfRequests: Long =>
        numberOfRequests should be > 0L
      }
    }
  }

  it should "honour DNS positive ttl when new connections are created" in withDockerizedEnvironment { (akkaClient, serverV1, serverV2) =>
    Given("server_v1, server_v2 and akka-client are up and running")
    And("akka-client is calling server_v1 only")
    eventually { akkaClient.startSendingRequests }
    eventually {
      whenReady(serverV1.receivedRequests()) { numberOfRequests: Long =>
        numberOfRequests should be > 0L
      }
      whenReady(serverV2.receivedRequests()) { numberOfRequest: Long =>
        numberOfRequest shouldBe 0L
      }
    }

    When("DNS entry has been changed and resolves to server_v2 IP")
    Docker.overrideExistingHostsEntry(akkaClient, "server.com", serverV2.ipAddress)

    And("The load is stopped for 'connection-idle' time, all connections should be killed")
    akkaClient.stopSendingRequests
    Thread.sleep(AkkaClientConfig.httpClientConnectionIdleTimeout.toMillis + 2000)
    akkaClient.startSendingRequests

    Then("akka-client is calling server_v2")
    eventually {
      whenReady(serverV2.receivedRequests()) { numberOfRequests: Long =>
        numberOfRequests should be > 0L
      }
    }
  }


  def withDockerizedEnvironment(testCode: (AkkaClient, Server, Server) => Any) {
    val serverV1 = Docker.startServer(name = "server_v1", hostPort = 8081)
    val serverV2 = Docker.startServer(name = "server_v2", hostPort = 8082)
    val akkaClient = Docker.startAkkaClient(downstreamDependencyIpAddress = serverV1.ipAddress, hostPort = 8080)

    try {
      testCode(akkaClient, serverV1, serverV2)
    }
    finally {
      Docker.destroy(akkaClient)
      Docker.destroy(serverV2)
      Docker.destroy(serverV1)
    }
  }
}
