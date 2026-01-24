package id.payu.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * AllServicesSimulation - Comprehensive performance test for all PayU services
 *
 * Scenario:
 * - Multiple scenarios running concurrently (Login, Transfer, QRIS, Balance)
 * - Ramp-up from 10 to 1000 concurrent users over 5 minutes
 * - Sustained load for 10 minutes
 * - Mixed workload simulating real user behavior
 *
 * Assertions:
 * - p95 response time < 1s for critical operations
 * - p99 response time < 2s for all operations
 * - Max response time < 5s for all operations
 * - Success rate > 99%
 */
class AllServicesSimulation extends Simulation {

  // Base URLs for different environments
  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val authUrl = System.getProperty("authUrl", s"$baseUrl/auth")
  val transactionUrl = System.getProperty("transactionUrl", s"$baseUrl/transaction")
  val walletUrl = System.getProperty("walletUrl", s"$baseUrl/wallet")

  // HTTP Protocol Configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("PayU-PerformanceTest/1.0")

  // Feeders for test data
  val userFeeder = csv("data/users.csv").circular
  val accountFeeder = csv("data/accounts.csv").circular

  // Random data generators
  val transferAmounts = Iterator.continually(Map(
    "amount" -> (10000 + (Math.random() * 990000).toInt).toString
  ))

  val qrisData = Iterator.continually(Map(
    "merchant_id" -> s"MERCHANT${(Math.random() * 100).toInt}",
    "amount" -> (10000 + (Math.random() * 990000).toInt).toString,
    "tip_amount" -> (Math.random() * 50000).toInt.toString
  ))

  // ========== SCENARIOS ==========

  // 1. Login Scenario (20% of users)
  val loginScenario = scenario("Login Scenario")
    .feed(userFeeder)
    .exec(
      http("Login")
        .post("$authUrl/api/v1/auth/login")
        .body(StringBody(
          """{
            "username": "${username}",
            "password": "${password}"
          }"""
        )).asJson
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("access_token"))
    )
    .pause(2, 5)

  // 2. Balance Query Scenario (30% of users)
  val balanceScenario = scenario("Balance Query Scenario")
    .feed(userFeeder)
    .feed(accountFeeder)
    .exec(
      http("Login")
        .post("$authUrl/api/v1/auth/login")
        .body(StringBody(
          """{
            "username": "${username}",
            "password": "${password}"
          }"""
        )).asJson
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("access_token"))
    )
    .pause(1, 2)
    .exec(
      http("Query Balance")
        .get("$walletUrl/api/v1/wallet/balance")
        .header("Authorization", "Bearer ${access_token}")
        .queryParam("account_number", "${account_number}")
        .check(status.is(200))
    )
    .pause(1, 3)
    .repeat(3) {
      exec(
        http("Repeated Query Balance")
          .get("$walletUrl/api/v1/wallet/balance")
          .header("Authorization", "Bearer ${access_token}")
          .queryParam("account_number", "${account_number}")
          .check(status.is(200))
      )
      .pause(1, 2)
    }

  // 3. Transfer Scenario (25% of users)
  val transferScenario = scenario("Transfer Scenario")
    .feed(userFeeder)
    .feed(accountFeeder)
    .feed(transferAmounts)
    .exec(
      http("Login")
        .post("$authUrl/api/v1/auth/login")
        .body(StringBody(
          """{
            "username": "${username}",
            "password": "${password}"
          }"""
        )).asJson
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("access_token"))
    )
    .pause(1, 2)
    .exec(
      http("Transfer")
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
    )
    .pause(3, 6)
    .repeat(2) {
      exec(
        http("Query Balance After Transfer")
          .get("$walletUrl/api/v1/wallet/balance")
          .header("Authorization", "Bearer ${access_token}")
          .queryParam("account_number", "${account_number}")
          .check(status.is(200))
      )
      .pause(2, 4)
    }

  // 4. QRIS Payment Scenario (25% of users)
  val qrisScenario = scenario("QRIS Payment Scenario")
    .feed(userFeeder)
    .feed(accountFeeder)
    .feed(qrisData)
    .exec(
      http("Login")
        .post("$authUrl/api/v1/auth/login")
        .body(StringBody(
          """{
            "username": "${username}",
            "password": "${password}"
          }"""
        )).asJson
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("access_token"))
    )
    .pause(1, 2)
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
    )
    .pause(1, 3)
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
    )
    .pause(4, 8)

  // ========== LOAD SIMULATION SETUP ==========

  setUp(
    // Login: 20% of total load
    loginScenario.inject(
      rampUsersPerSec(2) to 200 during (5 minutes),
      constantUsersPerSec(200) during (10 minutes)
    ).protocols(httpProtocol),

    // Balance Query: 30% of total load (most frequent operation)
    balanceScenario.inject(
      rampUsersPerSec(3) to 300 during (5 minutes),
      constantUsersPerSec(300) during (10 minutes)
    ).protocols(httpProtocol),

    // Transfer: 25% of total load
    transferScenario.inject(
      rampUsersPerSec(2.5) to 250 during (5 minutes),
      constantUsersPerSec(250) during (10 minutes)
    ).protocols(httpProtocol),

    // QRIS Payment: 25% of total load
    qrisScenario.inject(
      rampUsersPerSec(2.5) to 250 during (5 minutes),
      constantUsersPerSec(250) during (10 minutes)
    ).protocols(httpProtocol)
  ).assertions(
    // Global assertions
    global.responseTime.percentile3.lte(1000), // p95 < 1s
    global.responseTime.percentile4.lte(2000), // p99 < 2s
    global.responseTime.max.lte(5000),        // max < 5s
    global.successfulRequests.percent.gte(99), // success rate > 99%
    global.requestsPerSec.gte(100)            // at least 100 requests/sec sustained

    // Specific assertions for critical operations
    details("Login Scenario" / "Login").responseTime.percentile3.lte(1000),
    details("Balance Query Scenario" / "Query Balance").responseTime.percentile3.lte(500),
    details("Transfer Scenario" / "Transfer").responseTime.percentile3.lte(1000),
    details("QRIS Payment Scenario" / "Process QRIS Payment").responseTime.percentile3.lte(1500)
  )
}
