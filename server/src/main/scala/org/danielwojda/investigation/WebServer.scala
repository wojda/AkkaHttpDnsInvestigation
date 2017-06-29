package org.danielwojda.investigation
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.io.StdIn

object WebServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val hostname = java.net.InetAddress.getLocalHost.getHostName
    val getHostnameRequestsCounter = new AtomicLong()

    val route =
      get {
        path("hostname") {
          getHostnameRequestsCounter.incrementAndGet()
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, hostname))
        } ~
        path("counter") {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, getHostnameRequestsCounter.get().toString))
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}