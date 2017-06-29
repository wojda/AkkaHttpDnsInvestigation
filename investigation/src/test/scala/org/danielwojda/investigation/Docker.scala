package org.danielwojda.investigation

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.Materializer
import akka.util.ByteString
import com.github.dockerjava.api.model.{ExposedPort, Ports}
import com.github.dockerjava.core.command.ExecStartResultCallback
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientBuilder}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Docker {

  private val dockerConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
    .withDockerHost("unix:///var/run/docker.sock")
    .build()

  private val dockerClient = DockerClientBuilder.getInstance(dockerConfig).build()


  def startServer(name: String, hostPort: Int): Server with Containerized = {
    val tcp8080 = ExposedPort.tcp(8080)
    val portBindings = new Ports()
    portBindings.bind(tcp8080, Ports.Binding.bindPort(hostPort))

    val container = dockerClient.createContainerCmd("akka-investigation-server:0.1-SNAPSHOT")
      .withTty(true)
      .withExposedPorts(tcp8080)
      .withPortBindings(portBindings)
      .withHostName(name)
      .withName(name)
      .exec()

    dockerClient.startContainerCmd(container.getId).exec()

    val assignedIpAddress = dockerClient.inspectContainerCmd(container.getId).exec().getNetworkSettings.getNetworks.get("bridge").getIpAddress

    new Server(hostPort, assignedIpAddress) with Containerized {
        override val containerId: String = container.getId
      }
  }

  def startAkkaClient(downstreamDependencyIpAddress: String): Containerized = {
    val container = dockerClient.createContainerCmd("akka-investigation-client:0.1-SNAPSHOT")
      .withTty(true)
      .withName("akka-client")
      .withExtraHosts(s"server.com:$downstreamDependencyIpAddress")
      .exec()

    dockerClient.startContainerCmd(container.getId).exec()

    new Containerized {
      override val containerId: String = container.getId
    }
  }


  def destroy(containerized: Containerized) = {
    Try{dockerClient.killContainerCmd(containerized.containerId).exec()}
    Try{dockerClient.removeContainerCmd(containerized.containerId).exec()}
  }


  def overrideExistingHostsEntry(containerized: Containerized, hostname: String, newIp: String): Unit = {
    val cmd = dockerClient.execCreateCmd(containerized.containerId)
      .withCmd("/bin/bash", "-c", s"""sed '/$hostname/c\\$newIp $hostname' /etc/hosts > /etc/hosts.new; cp -f /etc/hosts.new /etc/hosts; rm /etc/hosts.new""")
      .exec()

    dockerClient.execStartCmd(cmd.getId).exec(new ExecStartResultCallback(System.out, System.err)).awaitCompletion()
  }
}


case class Server(boundPort: Int, ipAddress: String) {
  def receivedRequests()(implicit actorSystem: ActorSystem, mat: Materializer, ec: ExecutionContext): Future[Long] = {
    Http().singleRequest(HttpRequest(uri = Uri(s"http://localhost:$boundPort/counter")))
      .flatMap(response => response.entity.dataBytes.runFold(ByteString(""))(_ ++ _))
      .map(responseBody => responseBody.utf8String.toLong)
  }

}

trait Containerized {
  val containerId: String
}
