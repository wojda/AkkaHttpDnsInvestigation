package org.danielwojda.client

import java.net.InetAddress

import akka.actor.{Actor, ActorSystem, Cancellable, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.danielwojda.client.ClientApp.system
import org.danielwojda.client.HttpClientActor.{StartSendingRequests, StopSendingRequests}
import org.slf4j.LoggerFactory.getLogger

import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.language.postfixOps


object ClientApp extends App {

  implicit val system = ActorSystem("client-system")
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  val httpClientActor = system.actorOf(Props[HttpClientActor])

  val route =
    put {
      path("start") {
        httpClientActor ! StartSendingRequests
        complete(HttpEntity.Empty)
      } ~
      path("stop") {
        httpClientActor ! StopSendingRequests
        complete(HttpEntity.Empty)
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}

object HttpClientActor {
  case object StartSendingRequests
  case object StopSendingRequests
}

class HttpClientActor extends Actor {
  implicit val mat = ActorMaterializer()(context)
  implicit val ec = context.system.dispatcher
  val log = getLogger(getClass)

  override def receive: Receive = idleBehaviour

  def idleBehaviour: Receive = {
    case StartSendingRequests =>
      log.debug("Starting sending requests")
      val cancelableScheduler = context.system.scheduler.schedule(0 seconds, 100 milliseconds)(sendRequest())
      context.become(busyBehaviour(cancelableScheduler))
    case StopSendingRequests => log.warn("HttpClient is idle, nothing to stop")
  }

  def busyBehaviour(scheduler: Cancellable): Receive = {
    case StartSendingRequests => log.warn("HttpClient is already sending requests")
    case StopSendingRequests =>
      log.debug("stopping sending requests")
      scheduler.cancel()
      context.become(idleBehaviour)
  }

  def sendRequest() = {
    log.info(s"Checking explicitly what is server.com's IP address: ${InetAddress.getByName("server.com").getHostAddress}")

    Http().singleRequest(HttpRequest(uri = "http://server.com:8080/hostname")).onComplete {
      case Success(response) =>
        response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach {responseBody =>
          log.debug(responseBody.utf8String)
        }
      case Failure(e) =>
        log.warn(e.getMessage)
    }
  }

}