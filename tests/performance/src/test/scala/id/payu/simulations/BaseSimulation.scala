package id.payu.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * BaseSimulation - Abstract base class for all PayU performance simulations
 *
 * Provides common configuration and utilities for all performance tests:
 * - HTTP protocol configuration
 * - Common headers
 * - Feeders
 * - Assertion helpers
 */
abstract class BaseSimulation extends Simulation {

  // ========== CONFIGURATION ==========

  // Environment-specific URLs (can be overridden with system properties)
  val baseUrl: String = System.getProperty("baseUrl", "http://localhost:8080")
  val authUrl: String = System.getProperty("authUrl", s"$baseUrl/auth")
  val transactionUrl: String = System.getProperty("transactionUrl", s"$baseUrl/transaction")
  val walletUrl: String = System.getProperty("walletUrl", s"$baseUrl/wallet")
  val accountUrl: String = System.getProperty("accountUrl", s"$baseUrl/account")

  // Load test configuration
  val minUsers: Int = Integer.getInteger("minUsers", 10)
  val maxUsers: Int = Integer.getInteger("maxUsers", 1000)
  val rampUpDuration: Duration = Option(System.getProperty("rampUpDuration"))
    .map(d => FiniteDuration(d.toLong, "seconds"))
    .getOrElse(5 minutes)
  val sustainedDuration: Duration = Option(System.getProperty("sustainedDuration"))
    .map(d => FiniteDuration(d.toLong, "seconds"))
    .getOrElse(10 minutes)

  // Assertion thresholds
  val p95Threshold: Int = Integer.getInteger("p95Threshold", 1000) // 1s
  val p99Threshold: Int = Integer.getInteger("p99Threshold", 2000) // 2s
  val maxThreshold: Int = Integer.getInteger("maxThreshold", 5000) // 5s
  val successRateThreshold: Double = java.lang.Double.parseDouble(System.getProperty("successRateThreshold", "99.0"))

  // ========== HTTP PROTOCOL ==========

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.9,id;q=0.8")
    .contentTypeHeader("application/json")
    .userAgentHeader("PayU-PerformanceTest/1.0")

  // ========== COMMON HEADERS ==========

  val commonHeaders = Map(
    "Accept" -> "application/json",
    "Content-Type" -> "application/json",
    "User-Agent" -> "PayU-PerformanceTest/1.0"
  )

  val authHeaders = commonHeaders + (
    "Authorization" -> "Bearer ${access_token}"
  )

  // ========== COMMON CHECKS ==========

  val successCheck = status.in(200, 201, 202)
  val createdCheck = status.is(201)
  val okCheck = status.is(200)
  val unauthorizedCheck = status.is(401)
  val forbiddenCheck = status.is(403)
  val notFoundCheck = status.is(404)

  // JSON path checks
  val accessTokenCheck = jsonPath("$.access_token").exists().saveAs("access_token")
  val refreshTokenCheck = jsonPath("$.refresh_token").exists().saveAs("refresh_token")
  val userIdCheck = jsonPath("$.user_id").exists().saveAs("user_id")
  val transactionIdCheck = jsonPath("$.transaction_id").exists().saveAs("transaction_id")
  val accountNumberCheck = jsonPath("$.account_number").exists()
  val balanceCheck = jsonPath("$.available_balance").exists().saveAs("available_balance")
  val statusCheck = jsonPath("$.status").exists()

  // ========== COMMON REQUESTS ==========

  // Login request
  def loginRequest(username: String = "${username}", password: String = "${password}") = {
    http("Login")
      .post("$authUrl/api/v1/auth/login")
      .headers(commonHeaders)
      .body(StringBody(
        s"""{
           |"username": "$username",
           |"password": "$password"
           |}""".stripMargin
      )).asJson
      .check(okCheck)
      .check(accessTokenCheck)
      .check(refreshTokenCheck)
      .check(userIdCheck)
  }

  // Query balance request
  def queryBalanceRequest(accountNumber: String = "${account_number}") = {
    http("Query Balance")
      .get("$walletUrl/api/v1/wallet/balance")
      .headers(authHeaders)
      .queryParam("account_number", accountNumber)
      .check(okCheck)
      .check(accountNumberCheck)
      .check(balanceCheck)
  }

  // ========== COMMON ASSERTIONS ==========

  def globalAssertions = Seq(
    global.responseTime.percentile3.lte(p95Threshold),
    global.responseTime.percentile4.lte(p99Threshold),
    global.responseTime.max.lte(maxThreshold),
    global.successfulRequests.percent.gte(successRateThreshold)
  )

  def requestAssertions(requestName: String) = Seq(
    details(requestName).responseTime.percentile3.lte(p95Threshold),
    details(requestName).responseTime.percentile4.lte(p99Threshold),
    details(requestName).responseTime.max.lte(maxThreshold),
    details(requestName).successfulRequests.percent.gte(successRateThreshold)
  )

  // ========== LOAD INJECTION HELPERS ==========

  def standardRampUp = rampUsersPerSec(minUsers.toDouble) to maxUsers during (rampUpDuration)
  def standardSustainedLoad = constantUsersPerSec(maxUsers) during (sustainedDuration)
  def standardInjection = inject(standardRampUp, standardSustainedLoad)

  // ========== FEEDERS ==========

  val userFeeder = csv("data/users.csv").circular
  val accountFeeder = csv("data/accounts.csv").circular

  // Random amount generator (10,000 - 1,000,000)
  val randomAmountFeeder = Iterator.continually(Map(
    "amount" -> (10000 + (Math.random() * 990000).toInt).toString
  ))

  // Random merchant ID generator
  val randomMerchantFeeder = Iterator.continually(Map(
    "merchant_id" -> s"MERCHANT${(Math.random() * 100).toInt}",
    "store_id" -> s"STORE${(Math.random() * 500).toInt}"
  ))

  // Random QRIS data generator
  val randomQRISFeeder = Iterator.continually(Map(
    "merchant_id" -> s"MERCHANT${(Math.random() * 100).toInt}",
    "amount" -> (10000 + (Math.random() * 990000).toInt).toString,
    "tip_amount" -> (Math.random() * 50000).toInt.toString
  ))

  // ========== PAUSE HELPERS ==========

  def shortPause = pause(500.milliseconds, 1.seconds)
  def mediumPause = pause(1, 3)
  def longPause = pause(3, 6)

  // ========== THROTTLE HELPERS ==========

  def throttleRps(targetRps: Double) = throttle(
    reachRps(targetRps) in (rampUpDuration / 2),
    holdFor(sustainedDuration)
  )

  // ========== SCENARIO HELPERS ==========

  /**
   * Creates a scenario with authentication
   */
  def authenticatedScenario(name: String)(chain: ChainBuilder) = {
    scenario(name)
      .feed(userFeeder)
      .exec(loginRequest())
      .pause(1, 2)
      .exec(chain)
  }

  /**
   * Creates a scenario with authentication and account selection
   */
  def authenticatedAccountScenario(name: String)(chain: ChainBuilder) = {
    scenario(name)
      .feed(userFeeder)
      .feed(accountFeeder)
      .exec(loginRequest())
      .pause(1, 2)
      .exec(chain)
  }
}

/**
 * Simulation for quick smoke tests (lower load)
 */
trait SmokeTestSimulation extends BaseSimulation {
  override val minUsers: Int = 1
  override val maxUsers: Int = 10
  override val rampUpDuration: Duration = 30.seconds
  override val sustainedDuration: Duration = 2.minutes
}

/**
 * Simulation for stress testing (higher load)
 */
trait StressTestSimulation extends BaseSimulation {
  override val minUsers: Int = 100
  override val maxUsers: Int = 5000
  override val rampUpDuration: Duration = 15.minutes
  override val sustainedDuration: Duration = 30.minutes

  override val p95Threshold: Int = 2000 // 2s
  override val p99Threshold: Int = 5000 // 5s
  override val maxThreshold: Int = 10000 // 10s
  override val successRateThreshold: Double = 95.0 // 95%
}

/**
 * Simulation for soak testing (long duration)
 */
trait SoakTestSimulation extends BaseSimulation {
  override val minUsers: Int = 100
  override val maxUsers: Int = 500
  override val rampUpDuration: Duration = 10.minutes
  override val sustainedDuration: Duration = 2.hours

  override val p95Threshold: Int = 1500 // 1.5s
  override val successRateThreshold: Double = 99.5 // 99.5%
}

/**
 * Simulation for spike testing (sudden load increase)
 */
trait SpikeTestSimulation extends BaseSimulation {
  override val minUsers: Int = 50
  override val maxUsers: Int = 5000

  def spikeInjection = inject(
    constantUsersPerSec(minUsers) during (2.minutes),
    rampUsersPerSec(minUsers) to maxUsers during (1.minute),
    constantUsersPerSec(maxUsers) during (5.minutes),
    rampUsersPerSec(maxUsers) to minUsers during (1.minute),
    constantUsersPerSec(minUsers) during (2.minutes)
  )
}
