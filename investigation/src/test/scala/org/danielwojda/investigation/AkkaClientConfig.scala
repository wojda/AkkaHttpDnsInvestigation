package org.danielwojda.investigation

import scala.concurrent.duration.FiniteDuration

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * The object contains copied values from AkkaHttpDnsInvestigation/client/src/main/resources/application.conf
  *
  */
object AkkaClientConfig {
  val httpClientConnectionIdleTimeout: FiniteDuration = 15 seconds //akka.http.host-connection-pool.client.idle-timeout
  val akkaDnsPositiveTtl: FiniteDuration = 5 seconds //akka.io.dns.inet-address.positive-ttl
}
