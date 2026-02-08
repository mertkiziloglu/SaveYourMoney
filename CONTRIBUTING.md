# Contributing to SaveYourMoney

Thank you for your interest in contributing! This document provides guidelines for contributing to the project.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/SaveYourMoney.git`
3. Copy `.env.example` to `.env` and configure your environment
4. Start the services: `docker compose up`

## Development Setup

### Prerequisites
- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### Running Locally
```bash
# Build the analyzer service
cd analyzer-service && mvn clean compile

# Run tests
mvn test

# Start all services
docker compose up --build
```

## Code Standards

- **Type Safety**: `any` is forbidden — use explicit types everywhere
- **SOLID Principles**: Single responsibility per class, inject dependencies
- **Pattern Integrity**: Follow existing Strategy/Factory patterns
- **Test Coverage**: Every new service must include unit tests
- **Max Class Size**: 300 lines. If larger, refactor into smaller components

## Pull Request Process

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Write tests for new functionality
3. Ensure all tests pass: `mvn test`
4. Submit a PR with a clear description of changes

## Architecture

See [README.md](README.md) for the full architecture overview. Key points:

- **analyzer-service**: Core metrics collection and optimization engine
- **code-generator-service**: Generates IaC artifacts from recommendations
- **dashboard-ui**: Frontend visualization layer
- **demo-services**: Test microservices that simulate resource problems

## Security

Report security vulnerabilities privately — see [SECURITY.md](SECURITY.md).
