package id.payu.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * BalanceQuerySimulation - Performance test for wallet/balance service
 *
 * Scenario:
 * - Ramp-up from 10 to 1000 concurrent users over 5 minutes
 * - Sustained load for 10 minutes
 * - Each user performs login and balance queries
 *
 * Assertions:
 * - p95 response time < 500ms for balance query requests (read operation)
 * - p99 response time < 1s for balance query requests
 * - Max response time < 2s for balance query requests
 * - Success rate > 99.9%
 */
class BalanceQuerySimulation extends Simulation {

  // Base URLs for different environments
  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val authUrl = System.getProperty("authUrl", s"$baseUrl/auth")
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

  // Scenario Definition
  val balanceQueryScenario = scenario("Balance Query Scenario")
    .feed(userFeeder)
    .feed(accountFeeder)
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
    // Query main balance
    .exec(
      http("Query Main Balance")
        .get("$walletUrl/api/v1/wallet/balance")
        .header("Authorization", "Bearer ${access_token}")
        .queryParam("account_number", "${account_number}")
        .check(status.is(200))
        .check(jsonPath("$.account_number").is("${account_number}"))
        .check(jsonPath("$.available_balance").saveAs("available_balance"))
    )
    .pause(500.milliseconds, 2.seconds)
    // Query transaction history
    .exec(
      http("Query Transaction History")
        .get("$walletUrl/api/v1/wallet/transactions")
        .header("Authorization", "Bearer ${access_token}")
        .queryParam("account_number", "${account_number}")
        .queryParam("page", "0")
        .queryParam("size", "10")
        .check(status.is(200))
        .check(jsonPath("$.content").exists())
    )
    .pause(1, 3) // Think time between queries
    // Loop: Users repeatedly query balance (simulating real user behavior)
    .repeat(5) {
      exec(
        http("Repeated Balance Query")
          .get("$walletUrl/api/v1/wallet/balance")
          .header("Authorization", "Bearer ${access_token}")
          .queryParam("account_number", "${account_number}")
          .check(status.is(200))
      )
      .pause(1, 2)
    }

  // Load Simulation Setup
  setUp(
    balanceQueryScenario.inject(
      // Ramp-up from 10 to 1000 users over 5 minutes
      rampUsersPerSec(10) to 1000 during (5 minutes),
      // Sustained load for 10 minutes
      constantUsersPerSec(1000) during (10 minutes)
    )
  ).protocols(httpProtocol)
    .assertions(
      // Global assertions
      global.responseTime.percentile3.lte(500),  // p95 < 500ms (read operation)
      global.responseTime.percentile4.lte(1000), // p99 < 1s
      global.responseTime.max.lte(2000),         // max < 2s
      global.successfulRequests.percent.gte(99.9), // success rate > 99.9%

      // Specific to balance query requests
      details("Balance Query Scenario" / "Query Main Balance").responseTime.percentile3.lte(500),
      details("Balance Query Scenario" / "Query Main Balance").responseTime.percentile4.lte(1000),
      details("Balance Query Scenario" / "Query Main Balance").responseTime.max.lte(2000),
      details("Balance Query Scenario" / "Query Main Balance").successfulRequests.percent.gte(99.9),

      details("Balance Query Scenario" / "Query Transaction History").responseTime.percentile3.lte(800),
      details("Balance Query Scenario" / "Query Transaction History").responseTime.percentile4.lte(1500),
      details("Balance Query Scenario" / "Query Transaction History").responseTime.max.lte(3000),
      details("Balance Query Scenario" / "Query Transaction History").successfulRequests.percent.gte(99.9),

      details("Balance Query Scenario" / "Repeated Balance Query").responseTime.percentile3.lte(500),
      details("Balance Query Scenario" / "Repeated Balance Query").responseTime.percentile4.lte(1000),
      details("Balance Query Scenario" / "Repeated Balance Query").responseTime.max.lte(2000),
      details("Balance Query Scenario" / "Repeated Balance Query").successfulRequests.percent.gte(99.9)
    )
}
