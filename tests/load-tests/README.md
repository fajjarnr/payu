# =============================================================================
# PayU Load Tests — Consolidated Gatling Simulations
# =============================================================================
#
# This directory is the canonical location for all Gatling performance/load tests.
# Simulations previously in tests/performance/ are the source of truth.
#
# Quick Start:
#   cd tests/load-tests
#   mvn gatling:test -Dgatling.simulationClass=id.payu.simulations.AllServicesSimulation
#
# Individual simulations:
#   mvn gatling:test -Dgatling.simulationClass=id.payu.simulations.LoginSimulation
#   mvn gatling:test -Dgatling.simulationClass=id.payu.simulations.TransferSimulation
#   mvn gatling:test -Dgatling.simulationClass=id.payu.simulations.QRISPaymentSimulation
#   mvn gatling:test -Dgatling.simulationClass=id.payu.simulations.BalanceQuerySimulation
#
# Available Simulations:
#   - LoginSimulation          — Auth flow load test
#   - TransferSimulation       — P2P transfer throughput
#   - QRISPaymentSimulation    — QRIS payment flow
#   - BalanceQuerySimulation   — Read-heavy balance queries
#   - AllServicesSimulation    — Combined multi-service stress test
#
# See tests/performance/QUICK_START.md for detailed configuration.
# =============================================================================

NOTE: Simulation source files are in tests/performance/src/test/scala/
This pom.xml is configured to reference them via symlink or Maven sourceDirectory.
To run, use the performance directory directly:

    cd tests/performance
    mvn gatling:test

Or use the convenience script:

    ./tests/performance/run-performance-tests.sh
