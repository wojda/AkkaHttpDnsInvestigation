package org.danielwojda.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.io.StdIn
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.language.postfixOps


object ClientApp extends App {

  implicit val system = ActorSystem("client-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  system.scheduler.schedule(0 seconds, 100 milliseconds) {
    Http().singleRequest(HttpRequest(uri = "http://server.com:8080/hostname")).onComplete {
      case Success(response) =>
        response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach {responseBody =>
          println(responseBody.utf8String)
        }
      case Failure(e) =>
        println(e.getMessage)
    }
  }

  println("Press ENTER to stop")
  StdIn.readLine() // let it run until user presses return
}
