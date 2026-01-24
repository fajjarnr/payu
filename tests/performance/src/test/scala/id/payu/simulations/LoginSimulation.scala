package id.payu.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * LoginSimulation - Performance test for authentication service
 *
 * Scenario:
 * - Ramp-up from 10 to 1000 concurrent users over 5 minutes
 * - Sustained load for 10 minutes
 * - Each user attempts login
 *
 * Assertions:
 * - p95 response time < 1s for login requests
 * - p99 response time < 2s for login requests
 * - Max response time < 5s for login requests
 * - Success rate > 99%
 */
class LoginSimulation extends Simulation {

  // Base URLs for different environments
  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val authUrl = System.getProperty("authUrl", s"$baseUrl/auth")

  // HTTP Protocol Configuration
  val httpProtocol = http
    .baseUrl(authUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("PayU-PerformanceTest/1.0")

  // Feeders for test data
  val userFeeder = csv("data/users.csv").circular

  // Scenario Definition
  val loginScenario = scenario("Login Scenario")
    .feed(userFeeder)
    .exec(
      http("Login Request")
        .post("/api/v1/auth/login")
        .body(StringBody(
          """{
            "username": "${username}",
            "password": "${password}"
          }"""
        )).asJson
        .check(status.is(200))
        .check(jsonPath("$.access_token").saveAs("access_token"))
        .check(jsonPath("$.refresh_token").saveAs("refresh_token"))
        .check(jsonPath("$.user_id").saveAs("user_id"))
    )
    .pause(1, 3) // Think time between requests (1-3 seconds)

  // Load Simulation Setup
  setUp(
    loginScenario.inject(
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

      // Specific to login requests
      details("Login Scenario" / "Login Request").responseTime.percentile3.lte(1000),
      details("Login Scenario" / "Login Request").responseTime.percentile4.lte(2000),
      details("Login Scenario" / "Login Request").responseTime.max.lte(5000),
      details("Login Scenario" / "Login Request").successfulRequests.percent.gte(99)
    )
}
