# Look-see Audit Service

A Spring Boot service that processes audit progress events and publishes live audit updates for page and domain audits.

## Features

- Processes audit-related broker messages and converts them into user-facing progress updates.
- Supports both page and domain audit progress calculations.
- Broadcasts updates via `MessageBroadcaster` (for example, Pusher-backed real-time delivery).
- Uses Looksee core domain models/services for audit state, journeys, and scoring.

## Tech Stack

- Java 17
- Spring Boot 2.6.x
- Maven
- Looksee Core (`com.looksee:core`)

## Prerequisites

- Java 17+
- Maven 3.9+
- Access to the private `com.looksee:core` package (or a locally installed core JAR)

## Build and Run

### 1) Install Looksee Core locally (if not pulling from GitHub Packages)

```bash
mvn install:install-file -Dfile=libs/core-0.3.13.jar -DgroupId=com.looksee -DartifactId=core -Dversion=0.3.13 -Dpackaging=jar
```

### 2) Configure GitHub Packages auth (if needed)

```bash
export GITHUB_TOKEN=your_personal_access_token
export GITHUB_USERNAME=your_github_username
```

### 3) Build

```bash
mvn clean package
```

### 4) Run

```bash
mvn spring-boot:run
```

Or run the packaged jar:

```bash
java -jar target/audit-update-service-1.0.32.jar
```

## Service Endpoint

The service receives broker push payloads at:

```http
POST /
Content-Type: application/json
```

Expected request body shape:

```json
{
  "message": {
    "data": "<base64-encoded-json-message>"
  }
}
```

## Configuration

See `src/main/resources/application.properties` for runtime configuration. Key areas:

- Server and management ports
- GCP Pub/Sub topic names
- Pusher credentials (typically via env vars)
- Neo4j settings (commented templates)

## Code Review Findings (this repository)

### Issues fixed

1. **Potential null payload handling in controller**
   - Added defensive validation for null/invalid message payloads and now returns `400 Bad Request`.

2. **Unsafe `Optional#get()` usage**
   - Replaced risky direct `get()` usage with safer `ifPresent(...)` / guarded Optional checks in message-processing paths.

3. **Incorrect completion status condition**
   - Fixed duplicated progress condition checks to correctly include information architecture progress.

4. **Error status semantics**
   - Changed unknown/unhandled message response from `200 OK` to `400 Bad Request` for clearer client behavior.

5. **Logging and typo quality issues**
   - Replaced `printStackTrace()` with structured logger warnings and fixed typo in warning text.

6. **Broken test assertion typo**
   - Corrected `assetTrue(...)` typo and simplified the placeholder test to compile cleanly.

### Additional recommended follow-ups

- Add focused unit tests for `AuditController.receiveMessage(...)` covering each message type and malformed payloads.
- Consolidate message deserialization strategy to avoid repetitive try/catch branches.
- Replace field injection (`@Autowired`) with constructor injection for clearer immutability and testability.
- Resolve dependency retrieval/network policy issues that currently block Maven builds in restricted environments.

## Contributing

Contributions are welcome. Please follow Conventional Commits as documented in `CONTRIBUTING.md`.

## License

See `LICENSE`.
