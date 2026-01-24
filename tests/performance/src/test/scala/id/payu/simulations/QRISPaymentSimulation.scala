package id.payu.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * QRISPaymentSimulation - Performance test for QRIS payment service
 *
 * Scenario:
 * - Ramp-up from 10 to 1000 concurrent users over 5 minutes
 * - Sustained load for 10 minutes
 * - Each user performs login and QRIS payment operations
 *
 * Assertions:
 * - p95 response time < 1s for QRIS payment requests
 * - p99 response time < 2s for QRIS payment requests
 * - Max response time < 5s for QRIS payment requests
 * - Success rate > 99%
 */
class QRISPaymentSimulation extends Simulation {

  // Base URLs for different environments
  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val authUrl = System.getProperty("authUrl", s"$baseUrl/auth")
  val transactionUrl = System.getProperty("transactionUrl", s"$baseUrl/transaction")

  // HTTP Protocol Configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("PayU-PerformanceTest/1.0")

  // Feeders for test data
  val userFeeder = csv("data/users.csv").circular
  val accountFeeder = csv("data/accounts.csv").circular

  // QRIS merchant codes and amounts
  val qrisData = Iterator.continually(Map(
    "merchant_id" -> s"MERCHANT${(Math.random() * 100).toInt}",
    "amount" -> (10000 + (Math.random() * 990000).toInt).toString,
    "tip_amount" -> (Math.random() * 50000).toInt.toString
  ))

  // Scenario Definition
  val qrisScenario = scenario("QRIS Payment Scenario")
    .feed(userFeeder)
    .feed(accountFeeder)
    .feed(qrisData)
    // Login first to get token
    .exec(
      http("Login Request")
        .post("$authUrl/api/v1/auth/login")
        .body(StringBody(
          """{
            "username": "${username}",
            "password": "${password}"
          }"""
        )).asJson
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("access_token"))
        .check(jsonPath("$.user_id").saveAs("user_id"))
    )
    .pause(1, 2)
    // Create QRIS payment
    .exec(
      http("Create QRIS Payment")
        .post("$transactionUrl/api/v1/qris/create")
        .header("Authorization", "Bearer ${access_token}")
        .body(StringBody(
          """{
            "account_number": "${account_number}",
            "merchant_id": "${merchant_id}",
            "amount": ${amount},
            "tip_amount": ${tip_amount},
            "description": "Performance test QRIS payment"
          }"""
        )).asJson
        .check(status.in(200, 201))
        .check(jsonPath("$.qr_string").saveAs("qr_string"))
        .check(jsonPath("$.transaction_id").saveAs("qris_transaction_id"))
    )
    .pause(1, 3)
    // Simulate QR code scanning and payment
    .exec(
      http("Process QRIS Payment")
        .post("$transactionUrl/api/v1/qris/process")
        .header("Authorization", "Bearer ${access_token}")
        .body(StringBody(
          """{
            "qr_string": "${qr_string}",
            "pin": "123456"
          }"""
        )).asJson
        .check(status.in(200, 201))
        .check(jsonPath("$.status").is("SUCCESS"))
    )
    .pause(3, 6) // Think time between payments

  // Load Simulation Setup
  setUp(
    qrisScenario.inject(
      // Ramp-up from 10 to 1000 users over 5 minutes
      rampUsersPerSec(10) to 1000 during (5 minutes),
      // Sustained load for 10 minutes
      constantUsersPerSec(1000) during (10 minutes)
    )
  ).protocols(httpProtocol)
    .assertions(
      // Global assertions
      global.responseTime.percentile3.lte(1000), // p95 < 1s
      global.responseTime.percentile4.lte(2000), // p99 < 2s
      global.responseTime.max.lte(5000),        // max < 5s
      global.successfulRequests.percent.gte(99), // success rate > 99%

      // Specific to QRIS payment requests
      details("QRIS Payment Scenario" / "Create QRIS Payment").responseTime.percentile3.lte(1000),
      details("QRIS Payment Scenario" / "Create QRIS Payment").responseTime.percentile4.lte(2000),
      details("QRIS Payment Scenario" / "Create QRIS Payment").responseTime.max.lte(5000),
      details("QRIS Payment Scenario" / "Create QRIS Payment").successfulRequests.percent.gte(99),

      details("QRIS Payment Scenario" / "Process QRIS Payment").responseTime.percentile3.lte(1500),
      details("QRIS Payment Scenario" / "Process QRIS Payment").responseTime.percentile4.lte(3000),
      details("QRIS Payment Scenario" / "Process QRIS Payment").responseTime.max.lte(8000),
      details("QRIS Payment Scenario" / "Process QRIS Payment").successfulRequests.percent.gte(99)
    )
}
