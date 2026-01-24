package id.payu.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * TransferSimulation - Performance test for transfer service (BI-FAST)
 *
 * Scenario:
 * - Ramp-up from 10 to 1000 concurrent users over 5 minutes
 * - Sustained load for 10 minutes
 * - Each user performs login and transfer operations
 *
 * Assertions:
 * - p95 response time < 1s for transfer requests
 * - p99 response time < 2s for transfer requests
 * - Max response time < 5s for transfer requests
 * - Success rate > 99%
 */
class TransferSimulation extends Simulation {

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

  // Random amounts for transfers
  val transferAmounts = Iterator.continually(Map(
    "amount" -> (10000 + (Math.random() * 990000).toInt).toString
  ))

  // Scenario Definition
  val transferScenario = scenario("Transfer Scenario")
    .feed(userFeeder)
    .feed(accountFeeder)
    .feed(transferAmounts)
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
    // Perform transfer
    .exec(
      http("Transfer Request")
        .post("$transactionUrl/api/v1/transfers")
        .header("Authorization", "Bearer ${access_token}")
        .body(StringBody(
          """{
            "from_account": "${account_number}",
            "to_account": "ACC999001",
            "amount": ${amount},
            "description": "Performance test transfer",
            "transfer_type": "BI_FAST"
          }"""
        )).asJson
        .check(status.in(200, 201))
        .check(jsonPath("$.transaction_id").saveAs("transaction_id"))
        .check(jsonPath("$.status").is("PENDING"))
    )
    .pause(2, 5) // Think time between transfers

  // Load Simulation Setup
  setUp(
    transferScenario.inject(
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

      // Specific to transfer requests
      details("Transfer Scenario" / "Transfer Request").responseTime.percentile3.lte(1000),
      details("Transfer Scenario" / "Transfer Request").responseTime.percentile4.lte(2000),
      details("Transfer Scenario" / "Transfer Request").responseTime.max.lte(5000),
      details("Transfer Scenario" / "Transfer Request").successfulRequests.percent.gte(99)
    )
}
