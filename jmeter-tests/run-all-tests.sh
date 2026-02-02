#!/bin/bash

# SaveYourMoney - JMeter Test Orchestration Script
# Runs all load tests sequentially and generates reports

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
JMETER_HOME=${JMETER_HOME:-/usr/local/bin}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/scripts"
RESULTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/results"
REPORTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/reports"

# Service URLs
CPU_HUNGRY_URL=${CPU_HUNGRY_URL:-http://localhost:8081}
MEMORY_LEAKER_URL=${MEMORY_LEAKER_URL:-http://localhost:8082}
DB_CONNECTION_URL=${DB_CONNECTION_URL:-http://localhost:8083}

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}SaveYourMoney - JMeter Load Tests${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Create directories
mkdir -p "$RESULTS_DIR" "$REPORTS_DIR"

# Function to check if service is running
check_service() {
    local url=$1
    local name=$2

    echo -e "${YELLOW}Checking $name...${NC}"
    if curl -s -f "$url/api/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ $name is running${NC}"
        return 0
    else
        echo -e "${RED}✗ $name is NOT running at $url${NC}"
        return 1
    fi
}

# Function to run JMeter test
run_test() {
    local test_name=$1
    local jmx_file=$2

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Running: $test_name${NC}"
    echo -e "${GREEN}========================================${NC}"

    local timestamp=$(date +%Y%m%d-%H%M%S)
    local result_file="$RESULTS_DIR/${test_name}-${timestamp}.jtl"
    local report_dir="$REPORTS_DIR/${test_name}-${timestamp}"

    # Run JMeter test
    jmeter -n -t "$SCRIPT_DIR/$jmx_file" \
        -l "$result_file" \
        -e -o "$report_dir" \
        -Jjmeter.save.saveservice.output_format=csv \
        -Jjmeter.save.saveservice.response_data=false

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Test completed: $test_name${NC}"
        echo -e "${GREEN}  Results: $result_file${NC}"
        echo -e "${GREEN}  Report: $report_dir/index.html${NC}"
    else
        echo -e "${RED}✗ Test failed: $test_name${NC}"
        return 1
    fi
}

# Check if JMeter is installed
if ! command -v jmeter &> /dev/null; then
    echo -e "${RED}ERROR: JMeter is not installed or not in PATH${NC}"
    echo -e "${YELLOW}Install JMeter: brew install jmeter (Mac) or download from https://jmeter.apache.org${NC}"
    exit 1
fi

echo -e "${GREEN}JMeter version:${NC}"
jmeter --version

# Check if all services are running
echo ""
echo -e "${YELLOW}Checking services...${NC}"
services_ok=true

check_service "$CPU_HUNGRY_URL" "CPU-Hungry-Service (8081)" || services_ok=false
check_service "$MEMORY_LEAKER_URL" "Memory-Leaker-Service (8082)" || services_ok=false
check_service "$DB_CONNECTION_URL" "DB-Connection-Service (8083)" || services_ok=false

if [ "$services_ok" = false ]; then
    echo ""
    echo -e "${RED}ERROR: Not all services are running!${NC}"
    echo -e "${YELLOW}Start services before running tests.${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}All services are running! Starting tests...${NC}"
echo ""

# Run tests sequentially
run_test "cpu-hungry-service" "cpu-hungry-service.jmx"
sleep 5

run_test "memory-leaker-service" "memory-leaker-service.jmx"
sleep 5

run_test "db-connection-service" "db-connection-service.jmx"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All tests completed!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${GREEN}Results directory: $RESULTS_DIR${NC}"
echo -e "${GREEN}Reports directory: $REPORTS_DIR${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo -e "1. Review test results in the reports directory"
echo -e "2. Run analyzer service: curl -X POST http://localhost:8084/api/analyze-all"
echo -e "3. Generate optimized configurations"
echo ""
